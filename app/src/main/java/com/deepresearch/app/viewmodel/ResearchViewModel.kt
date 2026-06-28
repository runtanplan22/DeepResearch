package com.deepresearch.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepresearch.app.DeepResearchApp
import com.deepresearch.app.data.local.ReportCacheEntity
import com.deepresearch.app.data.model.*
import com.deepresearch.app.data.repository.ResearchRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Main ViewModel handling all research state and business logic.
 */
class ResearchViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DeepResearchApp
    private val settings = app.settingsDataStore

    private val repository = ResearchRepository(
        object : ResearchRepository.SettingsProvider(
            deepSeekBaseUrl = { settings.getDeepSeekBaseUrl() },
            deepSeekApiKey = { settings.getDeepSeekApiKey() },
            searxngBaseUrl = { settings.getSearxngBaseUrl() },
            searxngApiKey = { settings.getSearxngApiKey() }
        )
    )

    private val _researchState = MutableStateFlow(ResearchState())
    val researchState: StateFlow<ResearchState> = _researchState.asStateFlow()

    // Discussion chat state
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Settings UI state
    private val _deepSeekModels = MutableStateFlow<List<String>>(emptyList())
    val deepSeekModels: StateFlow<List<String>> = _deepSeekModels.asStateFlow()

    private val _deepSeekTestResult = MutableStateFlow<String?>(null)
    val deepSeekTestResult: StateFlow<String?> = _deepSeekTestResult.asStateFlow()

    private val _searxngTestResult = MutableStateFlow<String?>(null)
    val searxngTestResult: StateFlow<String?> = _searxngTestResult.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    init {
        // Try to restore last report from cache
        viewModelScope.launch {
            try {
                val cached = app.reportCacheDao.getReport()
                if (cached != null) {
                    _researchState.value = _researchState.value.copy(
                        topic = cached.topic,
                        report = cached.report,
                        status = ResearchStatus.COMPLETE
                    )
                }
            } catch (_: Exception) { /* no cache */ }
        }
    }

    /**
     * Update the research topic text.
     */
    fun updateTopic(topic: String) {
        _researchState.value = _researchState.value.copy(topic = topic)
    }

    /**
     * Update the report length.
     */
    fun updateReportLength(length: ReportLength) {
        _researchState.value = _researchState.value.copy(reportLength = length)
        settings.setDefaultReportLength(length.name)
    }

    /**
     * Update the iteration mode.
     */
    fun updateIterationMode(mode: IterationMode) {
        _researchState.value = _researchState.value.copy(iterationMode = mode)
        settings.setDefaultIterationMode(mode.name)
    }

    /**
     * Set manual iteration count.
     */
    fun updateManualIterationCount(count: Int) {
        settings.setManualIterationCount(count)
    }

    /**
     * Start the planning phase. Creates a research plan via the AI.
     */
    fun startPlanning() {
        val topic = _researchState.value.topic.trim()
        if (topic.isBlank()) return

        viewModelScope.launch {
            _researchState.value = _researchState.value.copy(
                status = ResearchStatus.PLANNING,
                isPlanning = true,
                error = null
            )

            try {
                val plan = repository.generatePlan(topic)
                _researchState.value = _researchState.value.copy(
                    plan = plan,
                    status = ResearchStatus.AWAITING_CONFIRMATION,
                    isPlanning = false
                )
            } catch (e: Exception) {
                _researchState.value = _researchState.value.copy(
                    status = ResearchStatus.ERROR,
                    isPlanning = false,
                    error = "Fehler bei der Planerstellung: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * User confirmed the plan — start the actual research.
     */
    fun confirmAndStartResearch() {
        val plan = _researchState.value.plan ?: return
        val mode = _researchState.value.iterationMode
        val totalIterations = if (mode == IterationMode.MANUAL) {
            settings.getManualIterationCount()
        } else {
            plan.estimatedIterations
        }

        viewModelScope.launch {
            _researchState.value = _researchState.value.copy(
                status = ResearchStatus.RESEARCHING,
                isRunning = true,
                totalIterations = totalIterations,
                currentIteration = 0,
                iterations = emptyList(),
                collectedImages = emptyList(),
                selectedImageUrls = emptySet(),
                report = ""
            )

            val allIterations = mutableListOf<IterationResult>()
            val allImages = mutableListOf<SearchImage>()
            var done = false
            var iterationNum = 0
            var currentQuery = plan.searchDirections.firstOrNull() ?: plan.topic

            while (!done && iterationNum < totalIterations) {
                iterationNum++
                _researchState.value = _researchState.value.copy(
                    currentIteration = iterationNum
                )

                try {
                    val result = repository.executeIteration(
                        query = currentQuery,
                        iterationNumber = iterationNum,
                        totalIterations = totalIterations,
                        context = allIterations.toList(),
                        mode = mode,
                        reportLength = _researchState.value.reportLength
                    )

                    allIterations.add(result)
                    allImages.addAll(result.intermediateImages)

                    _researchState.value = _researchState.value.copy(
                        iterations = allIterations.toList(),
                        collectedImages = allImages.distinctBy { it.imgUrl }
                    )

                    // Determine next direction or stop
                    currentQuery = result.nextDirection
                    if (currentQuery.uppercase() == "FINAL" || currentQuery.isBlank()) {
                        done = true
                    }

                    // Auto-mode: check if we should continue
                    if (mode == IterationMode.AUTO && iterationNum >= 3) {
                        // In auto mode, the AI decides when sufficient
                        if (result.nextDirection.uppercase() == "FINAL") {
                            done = true
                        }
                    }

                    // Small delay to let UI update
                    delay(300)
                } catch (e: Exception) {
                    _researchState.value = _researchState.value.copy(
                        status = ResearchStatus.ERROR,
                        isRunning = false,
                        error = "Fehler in Iteration $iterationNum: ${e.localizedMessage}"
                    )
                    return@launch
                }
            }

            // All iterations done — generate the final report
            _researchState.value = _researchState.value.copy(
                status = ResearchStatus.GENERATING_REPORT,
                isRunning = false
            )

            try {
                val selectedImages = _researchState.value.collectedImages
                    .filter { it.imgUrl in _researchState.value.selectedImageUrls }

                val report = repository.generateReport(
                    topic = _researchState.value.topic,
                    iterations = allIterations.toList(),
                    reportLength = _researchState.value.reportLength,
                    selectedImages = selectedImages
                )

                // Cache the report
                app.reportCacheDao.saveReport(
                    ReportCacheEntity(
                        topic = _researchState.value.topic,
                        report = report
                    )
                )

                _researchState.value = _researchState.value.copy(
                    report = report,
                    status = ResearchStatus.COMPLETE
                )
            } catch (e: Exception) {
                _researchState.value = _researchState.value.copy(
                    status = ResearchStatus.ERROR,
                    error = "Fehler bei der Berichtserstellung: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Toggle image selection for inclusion in the report.
     */
    fun toggleImageSelection(imgUrl: String) {
        val current = _researchState.value.selectedImageUrls.toMutableSet()
        if (current.contains(imgUrl)) {
            current.remove(imgUrl)
        } else {
            current.add(imgUrl)
        }
        _researchState.value = _researchState.value.copy(selectedImageUrls = current)
    }

    /**
     * Send a chat message for discussion about the report.
     */
    fun sendChatMessage(message: String) {
        val report = _researchState.value.report
        if (report.isBlank() || message.isBlank()) return

        val userMsg = ChatMessage(role = ChatRole.USER, content = message)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            try {
                val history = _chatMessages.value
                val answer = repository.generateDiscussionAnswer(report, message, history)
                val assistantMsg = ChatMessage(role = ChatRole.ASSISTANT, content = answer)
                _chatMessages.value = _chatMessages.value + assistantMsg
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = "Fehler: ${e.localizedMessage ?: "Konnte keine Antwort generieren"}"
                )
                _chatMessages.value = _chatMessages.value + errorMsg
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    /**
     * Reset everything for a new research.
     */
    fun resetResearch() {
        _researchState.value = ResearchState()
        _chatMessages.value = emptyList()
        _isChatLoading.value = false
    }

    // ---------- Settings API methods ----------

    fun loadDeepSeekModels() {
        viewModelScope.launch {
            _isLoadingModels.value = true
            try {
                val models = repository.loadModels()
                _deepSeekModels.value = models
            } catch (e: Exception) {
                _deepSeekModels.value = emptyList()
            } finally {
                _isLoadingModels.value = false
            }
        }
    }

    fun testDeepSeekConnection() {
        repository.resetDeepSeekService()
        viewModelScope.launch {
            _deepSeekTestResult.value = "Teste Verbindung..."
            try {
                val result = repository.testDeepSeekConnection()
                _deepSeekTestResult.value = result
            } catch (e: Exception) {
                _deepSeekTestResult.value = "❌ Fehler: ${e.localizedMessage}"
            }
        }
    }

    fun testSearXNGConnection() {
        viewModelScope.launch {
            _searxngTestResult.value = "Teste Verbindung..."
            try {
                val result = repository.testSearXNGConnection()
                _searxngTestResult.value = result
            } catch (e: Exception) {
                _searxngTestResult.value = "❌ Fehler: ${e.localizedMessage}"
            }
        }
    }

    fun clearTestResults() {
        _deepSeekTestResult.value = null
        _searxngTestResult.value = null
    }

    fun updateDeepSeekBaseUrl(url: String) {
        settings.setDeepSeekBaseUrl(url)
        repository.resetDeepSeekService()
    }

    fun updateDeepSeekApiKey(key: String) {
        settings.setDeepSeekApiKey(key)
        repository.resetDeepSeekService()
    }

    fun updateSelectedModel(model: String) {
        settings.setSelectedModel(model)
    }

    fun updateSearxngBaseUrl(url: String) {
        settings.setSearxngBaseUrl(url)
    }

    fun updateSearxngApiKey(key: String) {
        settings.setSearxngApiKey(key)
    }
}
