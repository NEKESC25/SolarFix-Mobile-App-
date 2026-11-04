package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.model.MaintenanceTask
import com.example.data.model.SolarSystemProfile
import com.example.data.model.TechnicianContact
import com.example.data.repository.SolarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SystemHubViewModel @JvmOverloads constructor(
    private val repository: SolarRepository = SolarRepository()
) : ViewModel() {

    val userProfile: StateFlow<SolarSystemProfile> = repository.userProfile
    val maintenanceTasks: StateFlow<List<MaintenanceTask>> = repository.maintenanceTasks
    val technicians: List<TechnicianContact> = repository.certifiedTechnicians

    private val _selectedTechnician = MutableStateFlow<TechnicianContact?>(null)
    val selectedTechnician: StateFlow<TechnicianContact?> = _selectedTechnician.asStateFlow()

    private val _isEditingProfile = MutableStateFlow(false)
    val isEditingProfile: StateFlow<Boolean> = _isEditingProfile.asStateFlow()

    fun toggleMaintenanceTask(taskId: String) {
        repository.toggleMaintenanceTask(taskId)
    }

    fun selectTechnician(technician: TechnicianContact?) {
        _selectedTechnician.value = technician
    }

    fun setEditingProfile(editing: Boolean) {
        _isEditingProfile.value = editing
    }

    fun saveProfile(
        kva: Double,
        batteryType: String,
        batteryCapacityKwh: Double,
        panelWatts: Int,
        installer: String
    ) {
        repository.updateProfile(
            SolarSystemProfile(
                systemSizeKva = kva,
                batteryType = batteryType,
                batteryCapacityKwh = batteryCapacityKwh,
                solarPanelWatts = panelWatts,
                installerName = installer
            )
        )
        _isEditingProfile.value = false
    }

    fun getCompletedTasksCount(): Int {
        return maintenanceTasks.value.count { it.isCompleted }
    }
}
