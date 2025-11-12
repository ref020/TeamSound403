package com.alexmercerind.audire.api.Spotify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

import android.util.Log
import org.json.JSONArray
import java.net.URLEncoder
import android.content.Context

object SpotifyPlaylists {
    private const val API_BASE_URL = "https://api.spotify.com/v1"

    private val client = OkHttpClient()


    suspend fun doesPlaylistExist(context: Context, playlistId: String): Boolean = withContext(Dispatchers.IO) {
        val playlists = getUserPlaylists(context)
        playlists.forEach {
            Log.d("SpotifyTest", "User playlist: ${it.getString("name")} (${it.getString("id")}")
        }
        val found = playlists.any { it.getString("id") == playlistId }
        Log.d("SpotifyTest", "Playlist $playlistId found: $found")
        found
    }
    suspend fun getUserPlaylists(context: Context, limit: Int = 50): List<JSONObject> = withContext(Dispatchers.IO) {
        val accessToken = SpotifyAuth.ensureValidAccessToken(context)
        var playlists = mutableListOf<JSONObject>()
        var offset = 0

        while (true) {
            val url = "$API_BASE_URL/me/playlists?limit=$limit&offset=$offset"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) break
                val json = JSONObject(response.body!!.string())
                val items = json.getJSONArray("items")
                for (i in 0 until items.length()) {
                    playlists.add(items.getJSONObject(i))
                }
                if (items.length() < limit) break
                offset += limit
            }
        }
        Log.d("Playlists Testing", "Getting playlists is working")
        playlists
    }

    suspend fun createPlaylist(context: Context, playlistName: String, isPublic: Boolean = true): JSONObject? = withContext(Dispatchers.IO) {
        Log.d("Playlists Testing", "Creating playlist was called properly")

        val userProfile = SpotifyAuth.getCurrentUserProfile(context) ?: return@withContext null
        val userId = userProfile.getString("id")
        val accessToken = SpotifyAuth.ensureValidAccessToken(context)

        Log.d("Playlists Testing", "Returned to createPlaylist")
        val jsonBody = JSONObject()
        jsonBody.put("name", playlistName)
        jsonBody.put("public", isPublic)

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$API_BASE_URL/users/$userId/playlists")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            JSONObject(response.body!!.string())
        }
    }

    suspend fun addTrackToPlaylist(context: Context, playlistId: String, songTitle: String, artistName: String? = null, albumName: String? = null) = withContext(Dispatchers.IO) {
        val accessToken = SpotifyAuth.ensureValidAccessToken(context)
        val trackUri = searchTrackUri(context, songTitle, artistName, albumName)
        val url = "https://api.spotify.com/v1/playlists/$playlistId/tracks"

        if (trackUri.isNullOrBlank()) {
            Log.e("SpotifyService", "No valid track URI found for $songTitle")
            return@withContext
        }

        val jsonBody = JSONObject().apply {
            put("uris", JSONArray().apply {
                put(trackUri)
            })
        }

        Log.d("SongURI", "Adding: $trackUri to: $playlistId" )

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to add track: ${response.code}")
        }
        response.close()
    }

    suspend fun removeTrackFromPlaylist(context: Context, playlistId: String, songTitle: String, artistName: String? = null, albumName: String? = null) = withContext(Dispatchers.IO) {
        val accessToken = SpotifyAuth.ensureValidAccessToken(context)
        val trackUri = searchTrackUri(context,songTitle, artistName, albumName)
        val url = "https://api.spotify.com/v1/playlists/$playlistId/tracks"

        if (trackUri.isNullOrBlank()) {
            Log.e("SpotifyService", "No valid track URI found for $songTitle")
            return@withContext
        }

        val jsonBody = JSONObject().apply {
            put("tracks", JSONArray().apply{
                put(JSONObject().apply {
                    put("uri", trackUri)
                })
            })
        }

        Log.d("SongURI", "Removing: $trackUri from: $playlistId" )

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .delete(requestBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to remove track: ${response.code}")
        }
        response.close()
    }

    suspend fun searchTrackUri(context: Context, songName: String, artistName: String? = null, albumName: String? = null): String? = withContext(Dispatchers.IO) {
        val accessToken = SpotifyAuth.ensureValidAccessToken(context)
        val normalizedSong = normalize(songName)
        val query = normalizedSong + if (!artistName.isNullOrBlank()) " artist:$artistName" else ""
        val url = "https://api.spotify.com/v1/search?q=${URLEncoder.encode(query, "UTF-8")}&type=track&limit=50"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null

            val responseBody = response.body!!.string()
            val json = JSONObject(responseBody)

            // Check for errors first
            if (json.has("error")) {
                val error = json.getJSONObject("error")
                Log.e("SpotifySearch", "Error searching track: ${error.getString("message")}")
                return@withContext null
            }

            val items = JSONObject(responseBody)
                .getJSONObject("tracks")
                .getJSONArray("items")

            val normalizedQuery = normalize(songName)

            for (i in 0 until items.length()) {
                val track = items.getJSONObject(i)
                val trackName = normalize(track.getString("name"))

                if (normalizedQuery.split(" ").all { trackName.contains(it) }) {
                    return@withContext track.getString("uri")
                }
            }

            if (items.length() > 0) return@withContext items.getJSONObject(0).getString("uri")
            null
        }
    }

    suspend fun normalize(name: String): String {
        return name.lowercase()
            .replace(Regex("\\(feat\\.[^)]+\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[^a-z0-9 *]", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
