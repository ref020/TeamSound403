package com.alexmercerind.audire.api.genius

import com.google.gson.Gson
import com.alexmercerind.audire.api.genius.GeniusApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import android.util.Log

object GeniusService {

    // take song found by ShazamApi and send it through genius pulling out songs by the artist
    suspend fun getSongsByArtist(artistName: String): List<String> = withContext(Dispatchers.IO) {
        val json = GeniusApi.search(artistName)

        Log.d("GeniusService", "Raw Genius response: $json")

        val gson = Gson()
        val response: GeniusSearchResponse = gson.fromJson(json, GeniusSearchResponse::class.java)

        val hits: List<GeniusHit> = response.response.hits

        val songs = hits
            .filter {it.result.primary_artist.name.equals(artistName, ignoreCase = true)}
            .map {it.result.title}
            .take(5) // limits number of songs pulled

        Log.d("GeuisServie", "Parsed songs: $songs")
        songs
    }
}
