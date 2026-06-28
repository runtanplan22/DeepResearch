package com.deepresearch.app.data.model

/**
 * Represents the entire research state for the current session.
 */
data class ResearchState(
    val topic: String = "",
    val plan: ResearchPlan? = null,
    val iterations: List<IterationResult> = emptyList(),
    val currentIteration: Int = 0,
    val totalIterations: Int = 0,
    val isRunning: Boolean = false,
    val isPlanning: Boolean = false,
    val status: ResearchStatus = ResearchStatus.IDLE,
    val report: String = "",
    val collectedImages: List<SearchImage> = emptyList(),
    val selectedImageUrls: Set<String> = emptySet(),
    val iterationMode: IterationMode = IterationMode.AUTO,
    val reportLength: ReportLength = ReportLength.MEDIUM,
    val error: String? = null
)

enum class ResearchStatus {
    IDLE,
    PLANNING,
    AWAITING_CONFIRMATION,
    RESEARCHING,
    GENERATING_REPORT,
    COMPLETE,
    ERROR
}

enum class IterationMode {
    AUTO,
    MANUAL
}

enum class ReportLength(val label: String, val wordCountRange: String) {
    SHORT("Kurz", "~300-500 Wörter"),
    MEDIUM("Mittel", "~800-1200 Wörter"),
    LONG("Ausführlich", "~2000-4000 Wörter")
}

/**
 * The research plan generated before execution.
 */
data class ResearchPlan(
    val topic: String,
    val subQuestions: List<String>,
    val searchDirections: List<String>,
    val estimatedIterations: Int
)

/**
 * Result from one research iteration.
 */
data class IterationResult(
    val iterationNumber: Int,
    val totalIterations: Int,
    val searchQuery: String,
    val sources: List<SearchResult>,
    val analysis: String,
    val nextDirection: String,
    val intermediateImages: List<SearchImage> = emptyList()
)

/**
 * A search result from SearXNG.
 */
data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String
)

/**
 * An image found during search.
 */
data class SearchImage(
    val imgUrl: String,
    val sourceUrl: String,
    val title: String,
    val thumbnailUrl: String? = null
)

/**
 * Chat message for the discussion feature.
 */
data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ChatRole {
    USER,
    ASSISTANT
}
