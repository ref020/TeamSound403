package com.alexmercerind.audire.services

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import android.util.Log
import com.alexmercerind.audire.services.LyricsFormatter

class LyricsScraper {
    /**
     * Parses the provided JSON string and returns the URL of the first song hit.
     *
     * This function performs the following steps:
     * 1. Parses the JSON string into a JsonObject.
     * 2. Extracts song's URL from the JsonObject.
     * 3. Scrapes the lyrics from the song's URL.
     * 4. Formats the lyrics for better readability.
     * 5. Returns the formatted lyrics.
     * 6. Handles exceptions and returns a fallback message if an error occurs
     *
     * @param url The JSON string to parse.
     * @return The formatted lyrics found
     */
    suspend fun getLyricsFromGenius(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Parse JSON
                val jsonObject = JsonParser.parseString(url).asJsonObject
                val hits = jsonObject["response"].asJsonObject["hits"].asJsonArray

                if (hits.size() == 0) {
                    return@withContext "No songs found in Genius results."
                }

                // Get the first hit's URL (you can choose another if needed)
                val songUrl = hits[0].asJsonObject["result"].asJsonObject["url"].asString
                Log.d("songUrl", songUrl)

                //connect to genius
                val doc = Jsoup.connect(songUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("Cache-Control", "max-age=0")
                    .referrer("http://www.google.com")
                    .timeout(10000)
                    .get()


                //genius puts lyrics inside <div data-lyrics-container="true">
                //selects the applicable container from page
                val elements = doc.select("div[data-lyrics-container=true]")

                // Get raw HTML from Genius
                val rawHTML = elements.html()

                // Replace <br> tags with newline placeholder
                val textWithNewLines = rawHTML.replace(Regex("(?i)<br>"), "[LINEBREAK]")

                // Use Jsoup to parse the HTML and strip out any HTML tags
                var lyrics = Jsoup.parse(textWithNewLines).text()

                //replace newline placeholder with newline
                lyrics = lyrics.replace(Regex("\\[LINEBREAK\\]"), "\n")

                //return lyrics or fallback text
                if (lyrics.isNotEmpty()) {
                    LyricsFormatter.makePretty(lyrics) //formats lyrics
                } else {
                    "Lyrics not found"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Error fetching lyrics"
            }
        }
    }
}