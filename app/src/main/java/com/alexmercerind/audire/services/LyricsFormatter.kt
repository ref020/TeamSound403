package com.alexmercerind.audire.services

import com.alexmercerind.audire.services.LyricsScraper

class LyricsFormatter {

    companion object {
        /**
         * Cleans and formats raw lyrics text scraped from Genius.
         *
         * This function performs the following steps:
         * 1. Removes the initial "Contributors ... Lyrics" header.
         * 2. Replaces structural tags like [Verse], [Chorus], etc., with bolded, spaced text.
         * 3. Trims leading and trailing whitespace from the final output.
         *
         * @param rawLyrics The raw string obtained from the web scraper.
         * @return A cleaned and more visually appealing String of lyrics.
         */
        fun makePretty(rawLyrics: String): String {
            // Step 1: Find the first occurrence of "[". This is usually where the actual lyrics start.
            val firstBracket = rawLyrics.indexOf('[')

            // If no bracket is found, something is wrong with the format. Return the text as-is.
            if (firstBracket == -1) {
                return rawLyrics.trim()
            }

            // Step 2: Get the substring starting from the first bracket. This removes the header
            var formattedLyrics = rawLyrics.substring(firstBracket)

            // Step 3: Use a regular expression to find all bracketed tags (e.g., [Verse], [Chorus]).
            // We then replace them with the same text, but bolded and with newlines for spacing.
            val regex = Regex("\\[(.*?)\\]")
            formattedLyrics = formattedLyrics.replace(regex) { matchResult ->
                // matchResult.value is the full matched text, e.g., "[Chorus]"
                "\n\n${matchResult.value}\n"
            }

            // Step 4: Use another Regex to find two or more consecutive newline characters.
            // This will collapse multiple blank lines into a single one.
            val emptyLinesRegex = Regex("(\r\n|\n){2,}")
            formattedLyrics = formattedLyrics.replace(emptyLinesRegex, "\n")

            // Step 5: Trim any leading or trailing whitespace/newlines from the final string for a clean look.
            return formattedLyrics.trim()
        }
    }
}
