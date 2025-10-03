package com.alexmercerind.audire.api.genius

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import com.alexmercerind.audire.BuildConfig

object GeniusApi {
    private val client = OkHttpClient()
    private val token: String = BuildConfig.GENIUS_ACCESS_TOKEN

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
