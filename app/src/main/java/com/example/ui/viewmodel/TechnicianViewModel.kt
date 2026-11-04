package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatMessageDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.SolarfixDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.InverterBrand
import com.example.data.model.InverterErrorCode
import com.example.data.model.SymptomDiagnostic
import com.example.data.repository.SolarRepository
import com.example.util.MobileSecurityAndMonitoringManager
import com.example.util.SecuritySanitizer
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "TechnicianViewModel"

sealed interface AiChatState {
    data object Idle : AiChatState
    data object Loading : AiChatState
    data class Streaming(val partialResponse: String) : AiChatState
    data class Error(val message: String) : AiChatState
}

class TechnicianViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: SolarRepository = SolarRepository(),
    chatDao: ChatMessageDao? = null
) : AndroidViewModel(application) {

    private val dao: ChatMessageDao? = chatDao ?: try {
        SolarfixDatabase.getDatabase(application).chatMessageDao()
    } catch (e: Exception) {
        MobileSecurityAndMonitoringManager.logSecureError(TAG, "Failed to initialize SolarfixDatabase Room DAO", e)
        null
    }

    init {
        // Automatically load persistent chat history from Room database
        dao?.let { safeDao ->
            viewModelScope.launch {
                try {
                    safeDao.getAllMessages().collect { entities ->
                        if (entities.isNotEmpty()) {
                            _chatMessages.value = entities.map { it.toChatMessage() }
                        }
                    }
                } catch (e: Exception) {
                    MobileSecurityAndMonitoringManager.logSecureError(TAG, "Error collecting chat messages from Room", e)
                }
            }
        }
    }

    // Lazy initialization of Firebase AI Logic model per guidelines
    private val generativeModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    // Selected inverter brand for quick error code filtering
    private val _selectedBrand = MutableStateFlow(InverterBrand.GROWATT)
    val selectedBrand: StateFlow<InverterBrand> = _selectedBrand.asStateFlow()

    // Filtered error codes
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeErrorCode = MutableStateFlow<InverterErrorCode?>(null)
    val activeErrorCode: StateFlow<InverterErrorCode?> = _activeErrorCode.asStateFlow()

    private val _activeSymptom = MutableStateFlow<SymptomDiagnostic?>(null)
    val activeSymptom: StateFlow<SymptomDiagnostic?> = _activeSymptom.asStateFlow()

    // Chat Messages list
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome_msg",
                isUser = false,
                content = "👋 Hello! I am your AI Solar Technician. Describe any inverter fault, error beeps, battery drain, or solar panel issue, and I'll give you instant step-by-step diagnostic fixes and safety advice.",
                timestamp = currentTimeFormatted(),
                quickSuggestions = listOf(
                    "Why is my battery draining so fast at night?",
                    "Growatt Error 04 low battery fix",
                    "How to stop inverter tripping on pump startup?",
                    "Best charging voltage for 48V Felicity Lithium"
                )
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _aiChatState = MutableStateFlow<AiChatState>(AiChatState.Idle)
    val aiChatState: StateFlow<AiChatState> = _aiChatState.asStateFlow()

    fun setSelectedBrand(brand: InverterBrand) {
        _selectedBrand.value = brand
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = SecuritySanitizer.sanitizeSearchQuery(query)
    }

    fun selectErrorCode(errorCode: InverterErrorCode?) {
        _activeErrorCode.value = errorCode
    }

    fun selectSymptom(symptom: SymptomDiagnostic?) {
        _activeSymptom.value = symptom
    }

    fun getFilteredErrorCodes(): List<InverterErrorCode> {
        val query = _searchQuery.value.trim().lowercase()
        val brand = _selectedBrand.value
        return repository.errorCodes.filter { item ->
            val matchesBrand = item.brand == brand
            val matchesQuery = query.isEmpty() ||
                    item.code.lowercase().contains(query) ||
                    item.title.lowercase().contains(query) ||
                    item.probableCause.lowercase().contains(query)
            matchesBrand && matchesQuery
        }
    }

    fun getAllSymptoms(): List<SymptomDiagnostic> = repository.symptoms

    fun clearChat() {
        val welcome = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = false,
            content = "👋 Hello! I am your AI Solar Technician. Describe any inverter fault, error beeps, or snap a photo of your inverter screen, battery bank, or panels for automated visual diagnosis.",
            timestamp = currentTimeFormatted(),
            quickSuggestions = listOf(
                "Why is my battery draining so fast at night?",
                "Growatt Error 04 low battery fix",
                "How to stop inverter tripping on pump startup?",
                "Best charging voltage for 48V Felicity Lithium"
            )
        )
        _chatMessages.value = listOf(welcome)
        viewModelScope.launch {
            try {
                dao?.clearAllMessages()
                dao?.insertMessage(ChatMessageEntity.fromChatMessage(welcome))
            } catch (e: Exception) {
                MobileSecurityAndMonitoringManager.logSecureError(TAG, "Failed to clear or write chat history in Room", e)
            }
        }
    }

    fun sendMessage(userText: String, attachedPhoto: android.graphics.Bitmap? = null) {
        val cleanText = SecuritySanitizer.sanitizeRichDescription(userText, maxLength = SecuritySanitizer.MAX_CHAT_MESSAGE_LENGTH)
        val bitmapValidation = SecuritySanitizer.validateUploadedBitmap(attachedPhoto)
        if (!bitmapValidation.isValid) {
            _aiChatState.value = AiChatState.Error(
                MobileSecurityAndMonitoringManager.sanitizeUserFacingErrorMessage(bitmapValidation.errorMessage ?: "Invalid image upload.")
            )
            return
        }
        if (cleanText.isEmpty() && attachedPhoto == null) return

        val displayContent = if (cleanText.isNotEmpty()) cleanText else "📸 [Attached Solar Equipment Photo for Diagnostic Inspection]"

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            content = displayContent,
            timestamp = currentTimeFormatted(),
            attachedBitmap = attachedPhoto
        )

        val botMessageId = UUID.randomUUID().toString()
        val initialBotMessage = ChatMessage(
            id = botMessageId,
            isUser = false,
            content = "",
            timestamp = currentTimeFormatted(),
            isStreaming = true
        )

        _chatMessages.update { it + userMessage + initialBotMessage }
        _aiChatState.value = AiChatState.Loading

        viewModelScope.launch {
            try {
                dao?.insertMessage(ChatMessageEntity.fromChatMessage(userMessage))
            } catch (e: Exception) {
                MobileSecurityAndMonitoringManager.logSecureError(TAG, "Failed to save user message in Room", e)
            }
        }

        viewModelScope.launch {
            val systemContext = """
                You are Solarfix AI, an expert Solar Photovoltaic (PV) and Power Electronics Technician specializing in residential and commercial solar systems across Africa (Nigeria, Kenya, Ghana, South Africa, etc.).
                You specialize in:
                - Solar Inverters: Growatt, Felicity Solar, Must Power, Deye, Sunsynk, Luminous, Sukam, Victron, SRNE.
                - Batteries: Lithium LiFePO4 (48V / 24V), Tall Tubular Lead Acid, Gel, Deep Cycle SMF.
                - Solar Panels: Monocrystalline, Bifacial, Poly, Strings, MPPT sizing, Harmattan dust cleaning.
                - Protections: DC Breakers, SPDs, Earth Grounding, Changeover switches.
                
                When the user attaches a photo of their solar equipment:
                1. Inspect the image closely (identify equipment brand, model, screen error numbers, fault LEDs, cable gauges, terminal corrosion, or panel soiling).
                2. Explain the root cause in simple, practical English.
                3. Provide 3-4 numbered actionable diagnostic steps.
                4. Clarify if the owner can perform this DIY or needs a certified technician.
                5. Always emphasize electrical safety (switch off DC isolators, do not touch bare conductors).
            """.trimIndent()

            val promptText = if (attachedPhoto != null) {
                "$systemContext\n\n[USER ATTACHED SOLAR EQUIPMENT PHOTO FOR VISUAL FAULT DIAGNOSIS]\nUser Notes: ${cleanText.ifEmpty { "Please inspect this solar equipment, diagnose any fault, error code, or wiring issue, and guide me on how to fix it." }}"
            } else {
                "$systemContext\n\nUser Question: $cleanText"
            }

            val accumulatedText = StringBuilder()

            try {
                withTimeout(60_000L) {
                    if (attachedPhoto != null) {
                        val contentPayload = content {
                            text(promptText)
                            image(attachedPhoto)
                        }
                        generativeModel.generateContentStream(contentPayload).collect { chunk ->
                            val chunkText = chunk.text.orEmpty()
                            accumulatedText.append(chunkText)
                            val currentText = accumulatedText.toString()

                            _aiChatState.value = AiChatState.Streaming(currentText)
                            _chatMessages.update { list ->
                                list.map { msg ->
                                    if (msg.id == botMessageId) {
                                        msg.copy(content = currentText, isStreaming = true)
                                    } else msg
                                }
                            }
                        }
                    } else {
                        generativeModel.generateContentStream(promptText).collect { chunk ->
                            val chunkText = chunk.text.orEmpty()
                            accumulatedText.append(chunkText)
                            val currentText = accumulatedText.toString()

                            _aiChatState.value = AiChatState.Streaming(currentText)
                            _chatMessages.update { list ->
                                list.map { msg ->
                                    if (msg.id == botMessageId) {
                                        msg.copy(content = currentText, isStreaming = true)
                                    } else msg
                                }
                            }
                        }
                    }
                }

                val finalOutput = accumulatedText.toString()
                if (finalOutput.isNotBlank()) {
                    val suggestions = generateSuggestionsForQuery(cleanText)
                    val completedBotMsg = ChatMessage(
                        id = botMessageId,
                        isUser = false,
                        content = finalOutput,
                        timestamp = currentTimeFormatted(),
                        isStreaming = false,
                        quickSuggestions = suggestions
                    )
                    _chatMessages.update { list ->
                        list.map { msg ->
                            if (msg.id == botMessageId) completedBotMsg else msg
                        }
                    }
                    _aiChatState.value = AiChatState.Idle
                    viewModelScope.launch {
                        try {
                            dao?.insertMessage(ChatMessageEntity.fromChatMessage(completedBotMsg))
                        } catch (e: Exception) {
                            MobileSecurityAndMonitoringManager.logSecureError(TAG, "Failed to save completed bot message in Room", e)
                        }
                    }
                } else {
                    fallbackToKnowledgeBase(botMessageId, cleanText, attachedPhoto != null)
                }

            } catch (e: TimeoutCancellationException) {
                MobileSecurityAndMonitoringManager.logSecureError(TAG, "Streaming generation timed out", e)
                fallbackToKnowledgeBase(botMessageId, cleanText, attachedPhoto != null, "Request took longer than expected over connection. Here is diagnostic guidance:")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MobileSecurityAndMonitoringManager.logSecureError(TAG, "Firebase AI generation exception", e)
                fallbackToKnowledgeBase(botMessageId, cleanText, attachedPhoto != null, "Expert Offline Knowledge Base:")
            }
        }
    }

    private fun fallbackToKnowledgeBase(
        botMessageId: String,
        userText: String,
        hasPhoto: Boolean,
        prefix: String = ""
    ) {
        val query = userText.lowercase()
        val matchedError = repository.errorCodes.find {
            query.contains(it.code.lowercase()) || query.contains(it.title.lowercase())
        }
        val matchedSymptom = repository.symptoms.find {
            query.contains("drain") && it.id.contains("drain") ||
            query.contains("pump") && it.id.contains("pump") ||
            query.contains("shock") && it.id.contains("shock") ||
            query.contains("fan") && it.id.contains("fan") ||
            query.contains("yield") || query.contains("low")
        }

        val responseText = buildString {
            if (prefix.isNotEmpty()) {
                appendLine("⚡ $prefix\n")
            }
            if (hasPhoto) {
                appendLine("📸 **Visual Solar Inspection Analysis:**")
                appendLine("• **Equipment Detected:** Solar Power Hardware (Inverter Display / Battery Terminals / Array Combiner)")
                appendLine("• **Visual Inspection Findings:**")
                appendLine("  1. **Inverter Display & Status LEDs:** Verify if the Warning/Fault LED is blinking Red or yellow, and look for an error code (e.g. F04, Err 08, Batt Low).")
                appendLine("  2. **Cable Lugs & Terminals:** Check for corrosion, loose nuts, or blackened insulation caused by overheating from loose connections.")
                appendLine("  3. **Breaker Protection:** Verify that DC string isolator breakers and battery circuit breakers have not tripped.")
                appendLine("\n**Action Checklist:**")
                appendLine("• For terminal issues: Turn off DC isolator, isolate battery breaker, and clean contacts with wire brush.")
                appendLine("• If an error code is displayed, tap the 'Inverter Fault Codes' tab or ask me about the specific code.")
                appendLine("\n⚠️ **Safety Note:** Never work on live DC cables with bare hands. Always isolate first.")
            } else if (matchedError != null) {
                appendLine("🔧 **Diagnosis: ${matchedError.brand.displayName} - ${matchedError.code} (${matchedError.title})**")
                appendLine("• **Probable Cause:** ${matchedError.probableCause}")
                appendLine("\n**Step-by-Step Fix:**")
                matchedError.stepByStepFix.forEachIndexed { i, step ->
                    appendLine("${i + 1}. $step")
                }
                if (matchedError.safetyWarning != null) {
                    appendLine("\n⚠️ **Safety Warning:** ${matchedError.safetyWarning}")
                }
            } else if (matchedSymptom != null) {
                appendLine("🔧 **Symptom Diagnosis: ${matchedSymptom.title}**")
                appendLine("• **Quick Assessment:** ${matchedSymptom.quickDiagnosis}")
                appendLine("\n**Recommended Solution:**")
                matchedSymptom.steps.forEachIndexed { i, step ->
                    appendLine("${i + 1}. $step")
                }
            } else {
                appendLine("⚡ **Solar Diagnostic Assessment:**")
                appendLine("Based on your solar setup query:")
                appendLine("1. **Check DC Isolator & Breakers:** Verify that your solar array DC breaker has not tripped and connections are securely tightened.")
                appendLine("2. **Inspect Battery Voltage:** For a 48V system, resting voltage should be 51.2V–53.5V for Lithium, or 48.0V–50.4V for Lead Acid/Tubular.")
                appendLine("3. **Shed Induction Loads:** If the inverter trips on startup, check if inductive appliances (pumps, compressors) are causing heavy surge currents.")
                appendLine("4. **Ventilation:** Ensure at least 30cm of clearance around the inverter heatsink to prevent thermal derating.")
                appendLine("\nFor hands-on repairs or high-voltage DC issues, visit our **System Hub** to connect with a verified local technician.")
            }
        }

        val fallbackBotMsg = ChatMessage(
            id = botMessageId,
            isUser = false,
            content = responseText,
            timestamp = currentTimeFormatted(),
            isStreaming = false,
            quickSuggestions = listOf(
                "Find certified solar technician",
                "Check inverter error code list",
                "How to size battery for my load"
            )
        )

        _chatMessages.update { list ->
            list.map { msg ->
                if (msg.id == botMessageId) fallbackBotMsg else msg
            }
        }
        _aiChatState.value = AiChatState.Idle
        viewModelScope.launch {
            try {
                dao?.insertMessage(ChatMessageEntity.fromChatMessage(fallbackBotMsg))
            } catch (e: Exception) {
                MobileSecurityAndMonitoringManager.logSecureError(TAG, "Failed to save fallback bot message in Room", e)
            }
        }
    }

    private fun generateSuggestionsForQuery(query: String): List<String> {
        val q = query.lowercase()
        return when {
            q.contains("battery") -> listOf("How to balance lithium cells?", "Prevent lead-acid sulfation", "View lithium batteries in marketplace")
            q.contains("inverter") -> listOf("Why is inverter fan loud?", "Deye hybrid inverter specs", "How to wire generator auto-start")
            q.contains("panel") -> listOf("How often should I wash panels?", "Bifacial vs Monocrystalline", "Find DC Surge Protectors")
            else -> listOf("Explain Low Battery voltage alarm", "How to size borehole solar pump", "Check maintenance checklist")
        }
    }

    private fun currentTimeFormatted(): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }
}
