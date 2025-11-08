package com.alexmercerind.audire.api.Spotify

import android.content.Context
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.FormBody
import org.json.JSONObject
import android.util.Log

object SpotifyAuth {

    private const val CLIENT_ID = "658c017dff584b91b1b60c4da4798234"
    private const val CLIENT_SECRET = "96a28d2b44ae4f47890a1a2606083868"
    private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
    private const val API_BASE_URL = "https://api.spotify.com/v1"
    private const val REDIRECT_URI = "audire://spotify-auth"
    private val client = OkHttpClient()

    // --- Encrypted storage for tokens ---
    private fun getEncryptedPrefs(context: Context) =
        EncryptedSharedPreferences.create(
            context,
            "spotify_tokens",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    fun saveTokens(context: Context, accessToken: String, refreshToken: String) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().apply {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            apply()
        }
    }

    fun getAccessToken(context: Context): String? {
        val prefs = getEncryptedPrefs(context)
        return prefs.getString("access_token", null)
    }

    fun getRefreshToken(context: Context): String? {
        val prefs = getEncryptedPrefs(context)
        return prefs.getString("refresh_token", null)
    }

    suspend fun exchangeCodeForTokens(code: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        val formBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .build()

        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(formBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val json = JSONObject(response.body!!.string())
            val accessToken = json.getString("access_token")
            val refreshToken = json.getString("refresh_token")
            Pair(accessToken, refreshToken)
        }
    }

    suspend fun refreshAccessToken(refreshToken: String): String? = withContext(Dispatchers.IO) {
        val formBody = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .build()

        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(formBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            JSONObject(response.body!!.string()).getString("access_token")
        }
    }

    suspend fun ensureValidAccessToken(context: Context): String? {
        val refreshToken = getRefreshToken(context) ?: return null
        val newAccessToken = refreshAccessToken(refreshToken)
        if (newAccessToken != null) {
            saveTokens(context, newAccessToken, refreshToken)
        }
        return newAccessToken
    }

    suspend fun getCurrentUserProfile(context: Context): JSONObject? = withContext(Dispatchers.IO) {
        Log.d("Playlists Testing", "get user profile called")
        val accessToken = ensureValidAccessToken(context)
        Log.d("Playlists Testing", "Access token valid")
        val request = Request.Builder()
            .url("$API_BASE_URL/me")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        Log.d("SpotifyDebug", "Access token: $accessToken")
        Log.d("SpotifyDebug", "URL: $API_BASE_URL/me")
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful){
                val errorBody = response.body?.string()
                Log.d("SpotifyDebug", "User profile response failed: ${response.code} - $errorBody")
                return@withContext null
            }
            JSONObject(response.body!!.string())
        }
    }

    fun clearTokens(context: Context) {
        val prefs = context.getSharedPreferences("spotify_tokens", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
