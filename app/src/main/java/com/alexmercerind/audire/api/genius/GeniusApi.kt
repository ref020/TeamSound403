package com.alexmercerind.audire.api.genius

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import com.alexmercerind.audire.BuildConfig

object GeniusApi {
    private val client = OkHttpClient()
    private val token: String = BuildConfig.GENIUS_ACCESS_TOKEN
    /**
     * Searches Genius for the provided query.
     *
     * This function performs the following steps:
     * 1. Constructs the URL for the Genius search API.
     * 2. Creates an HTTP request with the URL and authorization header.
     * 3. Executes the request and processes the response.
     * 4. Returns the response body as a string.
     *
     * @param query the string to search for on the Genius website
     * @return webpage metadata of the searched song
     */
    fun search(query: String): String {
        val url = "https://api.genius.com/search?q=${query.replace(" ", "%20")}"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            return response.body?.string() ?: ""
        }
    }
}
