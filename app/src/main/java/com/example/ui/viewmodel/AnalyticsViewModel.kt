package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.model.Currency
import com.example.data.model.EnergyMetrics
import com.example.data.repository.SolarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DayPeriod(val label: String, val solarFactor: Float, val hourDesc: String) {
    MORNING("Early Morning", 0.35f, "08:30 AM • Sun Rising"),
    MIDDAY("Midday Peak", 1.0f, "01:15 PM • Peak Irradiation"),
    AFTERNOON("Late Afternoon", 0.65f, "04:30 PM • Sun Lowering"),
    NIGHT("Night / Off-Grid", 0.0f, "09:45 PM • Battery Power Only")
}

class AnalyticsViewModel @JvmOverloads constructor(
    private val repository: SolarRepository = SolarRepository()
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(DayPeriod.MIDDAY)
    val selectedPeriod: StateFlow<DayPeriod> = _selectedPeriod.asStateFlow()

    private val _customLoadAdjustmentWatts = MutableStateFlow(0)
    val customLoadAdjustmentWatts: StateFlow<Int> = _customLoadAdjustmentWatts.asStateFlow()

    val currency: StateFlow<Currency> = repository.selectedCurrency

    fun setDayPeriod(period: DayPeriod) {
        _selectedPeriod.value = period
    }

    fun toggleApplianceLoad(watts: Int, isEnabled: Boolean) {
        _customLoadAdjustmentWatts.update { current ->
            if (isEnabled) current + watts else (current - watts).coerceAtLeast(0)
        }
    }

    fun getComputedMetrics(): EnergyMetrics {
        val baseMetrics = repository.getCurrentEnergyMetrics()
        val factor = _selectedPeriod.value.solarFactor
        val computedSolar = (baseMetrics.solarWattsNow * factor).toInt()
        val totalLoad = (baseMetrics.loadWattsNow + _customLoadAdjustmentWatts.value).coerceAtLeast(100)

        val netPower = computedSolar - totalLoad
        val adjustedSoc = when {
            factor == 0.0f -> 62 // Drained down during night
            factor < 0.5f -> 75
            else -> 92
        }

        val hoursRemaining = if (netPower < 0) {
            val batteryCapacityWattHours = 5120.0 * (adjustedSoc / 100.0)
            val deficit = kotlin.math.abs(netPower).toDouble()
            (batteryCapacityWattHours / deficit).coerceIn(0.5, 24.0)
        } else {
            16.0 // Fully supported by solar
        }

        val fuelSavedLitres = 4.8 * (computedSolar.toDouble() / 2500.0).coerceIn(0.5, 8.0)

        return baseMetrics.copy(
            solarWattsNow = computedSolar,
            batterySocPercent = adjustedSoc,
            loadWattsNow = totalLoad,
            estimatedHoursLeftAtCurrentLoad = hoursRemaining,
            generatorFuelSavedLitres = fuelSavedLitres
        )
    }

    fun calculateFuelSavingsInCurrency(litresSaved: Double, currency: Currency): String {
        // Average petrol/diesel cost in Nigeria is ~₦1,150 / liter
        val costInNgn = (litresSaved * 1150.0).toLong()
        return currency.formatPrice(costInNgn)
    }
}
