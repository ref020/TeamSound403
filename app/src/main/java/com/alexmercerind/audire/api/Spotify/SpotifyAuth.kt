package com.alexmercerind.audire.api.Spotify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object SpotifyAuth {
    private const val CLIENT_ID = "658c017dff584b91b1b60c4da4798234"
    private const val CLIENT_SECRET = "96a28d2b44ae4f47890a1a2606083868"
    private const val REFRESH_TOKEN = "AQDMw6vR1KqZEkdvGQf8JZB_bhlhTV8E71_BLuyMbJVRV9puRVUasgDEL7GllQ-w4DNHYWTAEwLHa0uVo5KkEW5NVuaCaohwlTArDs5c0EXslf9xlf6ZibH66svCp03S79o"
    private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
    private const val API_BASE_URL = "https://api.spotify.com/v1"
    private val client = OkHttpClient()

    suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        val body = "grant_type=refresh_token&refresh_token=${REFRESH_TOKEN}&client_id=${CLIENT_ID}&client_secret=${CLIENT_SECRET}"
            .toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to get access token: ${response.code}")
            }
            val json = JSONObject(response.body!!.string())
            json.getString("access_token")
        }
    }

    suspend fun getCurrentUserProfile(): JSONObject? = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken()
        val request = Request.Builder()
            .url("$API_BASE_URL/me")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            JSONObject(response.body!!.string())
        }
    }

}