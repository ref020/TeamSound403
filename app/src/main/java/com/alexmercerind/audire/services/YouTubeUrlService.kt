package com.alexmercerind.audire.services
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object YouTubeUrlService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getYoutubeUrl(query: String, apiKey: String): String? {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val apiUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=1&q=$encodedQuery&key=$apiKey"

        val request = Request.Builder().url(apiUrl).get().build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("YoutubeService", "YouTube API failed: ${apiKey}")
                        return@withContext null
                    }

                    val json = JSONObject(response.body?.string() ?: "")
                    val items = json.optJSONArray("items")
                    if (items != null && items.length() > 0) {
                        val videoId = items.getJSONObject(0).getJSONObject("id").getString("videoId")
                        return@withContext "https://www.youtube.com/watch?v=$videoId"
                    }

                    Log.e("YoutubeService", "No results for: $query")
                    null
                }
            } catch (e: Exception) {
                Log.e("YoutubeService", "Error getting YouTube URL", e)
                null
            }
        }
    }
}
