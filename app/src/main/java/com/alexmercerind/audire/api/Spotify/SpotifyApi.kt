package com.alexmercerind.audire.api.Spotify

import okhttp3.*
import org.json.JSONObject
import java.io.IOException

object SpotifyApi {
    private const val API_BASE_URL = "https://api.spotify.com/v1"

    fun getCurrentUserProfile(accessToken: String, callback: (JSONObject?) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$API_BASE_URL/me")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(null)
                        return
                    }
                    callback(JSONObject(it.body!!.string()))
                }
            }
        })
    }
}