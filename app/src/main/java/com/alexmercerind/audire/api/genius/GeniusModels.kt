package com.alexmercerind.audire.api.genius

import com.alexmercerind.audire.api.genius.GeniusApi

data class GeniusSearchResponse(
    val response: GeniusHits
)

data class GeniusHits(
    val hits: List<GeniusHit>
)

data class GeniusHit(
    val result: GeniusSong
)

data class GeniusSong(
    val title: String,
    val primary_artist: GeniusArtist
)

data class GeniusArtist(
    val name: String
)