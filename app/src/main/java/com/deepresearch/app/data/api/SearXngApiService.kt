package com.deepresearch.app.data.api

import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for SearXNG search API.
 */
interface SearXngApiService {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("categories") categories: String = "general",
        @Query("pageno") pageNo: Int = 1,
        @Query("language") language: String = "de"
    ): Response<SearXNGSearchResponse>

    @GET("search")
    suspend fun searchWithImages(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("categories") categories: String = "images",
        @Query("pageno") pageNo: Int = 1
    ): Response<SearXNGSearchResponse>

    @GET("config")
    suspend fun getConfig(): Response<SearXNGConfigResponse>
}
