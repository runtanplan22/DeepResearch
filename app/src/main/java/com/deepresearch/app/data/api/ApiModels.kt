package com.deepresearch.app.data.api

import com.google.gson.annotations.SerializedName

/**
 * Request body for DeepSeek chat completions.
 */
data class DeepSeekChatRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = false,
    @SerializedName("max_tokens")
    val maxTokens: Int = 4096,
    val temperature: Double = 0.7
)

data class ChatMessageDto(
    val role: String,
    val content: String
)

/**
 * Response from DeepSeek chat completions endpoint.
 */
data class DeepSeekChatResponse(
    val id: String? = null,
    val choices: List<ChoiceDto>? = null,
    val error: ApiErrorDto? = null
)

data class ChoiceDto(
    val index: Int = 0,
    val message: ChatMessageDto? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class ApiErrorDto(
    val message: String? = null,
    val type: String? = null
)

/**
 * Response from DeepSeek models listing endpoint.
 */
data class ModelsResponse(
    val data: List<ModelDto>? = null,
    val error: ApiErrorDto? = null
)

data class ModelDto(
    val id: String,
    val owned_by: String? = null
)

/**
 * SearXNG search request parameters.
 */
data class SearXNGSearchRequest(
    val q: String,
    val format: String = "json",
    val categories: String = "general",
    val pageno: Int = 1,
    val language: String = "de"
)

/**
 * SearXNG search response.
 */
data class SearXNGSearchResponse(
    val query: String? = null,
    val results: List<SearXNGResultDto>? = null,
    val infoboxes: List<SearXNGInfoboxDto>? = null,
    val suggestions: List<String>? = null
)

data class SearXNGResultDto(
    val title: String? = null,
    val url: String? = null,
    val content: String? = null,
    val img_src: String? = null,
    val thumbnail_src: String? = null
)

data class SearXNGInfoboxDto(
    val infobox: String? = null,
    val content: String? = null,
    val img_src: String? = null,
    val urls: List<SearXNGUrlDto>? = null
)

data class SearXNGUrlDto(
    val title: String? = null,
    val url: String? = null
)

/**
 * Connection test response from SearXNG about/config.
 */
data class SearXNGConfigResponse(
    val version: String? = null,
    val instance_name: String? = null
)
