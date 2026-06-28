package com.deepresearch.app.data.api

import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for DeepSeek-compatible API calls.
 */
interface DeepSeekApiService {
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: DeepSeekChatRequest
    ): Response<DeepSeekChatResponse>

    @GET("v1/models")
    suspend fun listModels(
        @Header("Authorization") authHeader: String
    ): Response<ModelsResponse>
}
