package com.alexmercerind.audire.api.Spotify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import android.content.Context

import android.util.Log
import org.json.JSONArray
import java.net.URLEncoder

object SpotifyLikeSong {
    private const val API_BASE_URL = "https://api.spotify.com/v1"

    private val client = OkHttpClient()

    suspend fun likeSong(
        context: Context,
        songName: String,
        artistName: String? = null,
        albumName: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val accessToken = SpotifyAuth.ensureValidAccessToken(context) ?: return@withContext false
        val trackUri = searchTrackUri(context,songName, artistName, albumName)

        val json = """{"ids":["$trackUri"]}"""
        val body = json.toRequestBody("application/json".toMediaType())

        if (isSongLiked(trackUri, accessToken, songName)) return@withContext false
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/tracks")
            .addHeader("Authorization", "Bearer $accessToken")
            .put(body)
            .build()

        client.newCall(request).execute().use { response ->
            Log.d("SpotifyLikes", "Song added to liked = $response")
            response.isSuccessful
        }

    }

    suspend fun unlikeSong(
        context: Context,
        songName: String,
        artistName: String? = null,
        albumName: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val accessToken = SpotifyAuth.ensureValidAccessToken(context) ?: return@withContext false
        val trackUri = searchTrackUri(context, songName, artistName, albumName)

        val json = """{"ids":["$trackUri"]}"""
        val body = json.toRequestBody("application/json".toMediaType())

        if (!isSongLiked(trackUri, accessToken, songName)) return@withContext false
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/tracks")
            .addHeader("Authorization", "Bearer $accessToken")
            .delete(body)
            .build()

        client.newCall(request).execute().use { response ->
            Log.d("SpotifyLikes", "Song removed from liked = $response")
            response.isSuccessful
        }

    }

    suspend fun searchTrackUri(
        context: Context,
        songName: String,
        artistName: String? = null,
        albumName: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val accessToken = SpotifyAuth.ensureValidAccessToken(context) ?: return@withContext null
        val normalizedSong = normalize(songName)
        val query = normalizedSong + if (!artistName.isNullOrBlank()) " artist:$artistName" else ""
        val url = "https://api.spotify.com/v1/search?q=${
            URLEncoder.encode(
                query,
                "UTF-8"
            )
        }&type=track&limit=50"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null

            val items = JSONObject(response.body!!.string())
                .getJSONObject("tracks")
                .getJSONArray("items")

            val normalizedQuery = normalize(songName)

            for (i in 0 until items.length()) {
                val track = items.getJSONObject(i)
                val trackName = normalize(track.getString("name"))

                if (normalizedQuery.split(" ").all { trackName.contains(it) }) {
                    return@withContext track.getString("uri").substringAfterLast(":")
                }
            }

            if (items.length() > 0) return@withContext items.getJSONObject(0).getString("uri").substringAfterLast(":")
            null
        }
    }

    suspend fun normalize(name: String): String {
        return name.lowercase()
            .replace(Regex("\\(feat\\.[^)]+\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[^a-z0-9 *]", RegexOption.IGNORE_CASE), "")
            .trim()
    }


    suspend fun isSongLiked(trackUri: String?, accessToken: String, songName: String): Boolean = withContext(Dispatchers.IO) {
            val client = OkHttpClient()

            val request = Request.Builder()
                .url("https://api.spotify.com/v1/me/tracks/contains?ids=$trackUri")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                val liked = JSONArray(body).optBoolean(0, false)

                Log.d("SpotifyLikes", "Checking if song is liked: $songName liked = $liked")
                liked
            }
        }
}

