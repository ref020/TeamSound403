package com.alexmercerind.audire.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alexmercerind.audire.models.HistoryItem
import com.alexmercerind.audire.repository.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import android.util.Log
import com.alexmercerind.audire.api.Spotify.SpotifyPlaylists
import com.alexmercerind.audire.api.Spotify.SpotifyLikeSong
import android.content.Context
import kotlin.collections.emptyList

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    val historyItems: StateFlow<List<HistoryItem>?>
        get() = _historyItems

    private val _historyItems = MutableStateFlow<List<HistoryItem>?>(null)

    //var filterChoices1: List<String> = listOf()
    var filterType = MutableStateFlow<String?>(null)
    var filterChoice = MutableStateFlow<String?>(null)

    var filters = MutableStateFlow<List<List<String?>>>(emptyList())
    var sortChoice = MutableStateFlow<String?>(null)
    var isAscending = MutableStateFlow<Boolean>(true)
    var query = MutableStateFlow<String>("")


    fun filterSortSearch() {
        viewModelScope.launch {
            combine(
                query,
                filters,
                sortChoice,
                isAscending
            ) { query, filters, sortChoice, isAscending ->
                FilterSortSearchChoices(query, filters, sortChoice, isAscending)
            }.collect { choices ->
                mutex.withLock {
                    val searchItems: List<HistoryItem> = when (choices.query) {
                        "" -> getAll().first()
                        else -> search(choices.query.lowercase())
                    }
                    var filteredItems: List<HistoryItem> = emptyList()
                    println(choices.filters)

                    if (choices.filters.isEmpty()) {
                        filteredItems = searchItems
                        println("no filta")
                    } else {
                        println("unginggn")
                        println(choices.filters)
                        for (choice in choices.filters) {
                            println(choice)
                            when (choice[0]) {
                                "Artist" -> filteredItems += (searchItems.filter { it.artists == choice[1] })
                                "Album" -> filteredItems += (searchItems.filter { it.album == choice[1] })
                                "Year" -> filteredItems += (searchItems.filter { it.year == choice[1] })
                            }
                        }
                    }

                    var sortedItems: List<HistoryItem> = when (choices.sortChoice) {
                        "Date Added" -> filteredItems.sortedBy { it.timestamp }
                        "Title" -> filteredItems.sortedBy { it.title }
                        "Year" -> filteredItems.sortedBy { it.year }
                        "Artist" -> filteredItems.sortedBy { it.artists }
                        "Album" -> filteredItems.sortedBy { it.album }
                        else -> filteredItems
                    }

                    if (!choices.isAscending) {
                        sortedItems = sortedItems.reversed()
                    }
                    _historyItems.emit(sortedItems)
                    println("newitermemss")
                }

            }
        }
    }
    private val mutex = Mutex()

    private val repository = HistoryRepository(application)

    init {
        viewModelScope.launch {
            getAll().collect {
                _historyItems.emit(it)
            }

        }
    }


    private fun getAll() = repository.getAll()

    private suspend fun search(query: String) = repository.search(query)


    suspend fun getFilterChoices(): Flow<List<String>> {
        val filterArtistChoices = getFilterArtistChoices()
        val filterAlbumChoices = getFilterAlbumChoices()
        val filterYearChoices = getFilterYearChoices()
        return combine(MutableStateFlow("Clear Filters"), filterArtistChoices, filterAlbumChoices, filterYearChoices) { clearFilters, artistChoices, albumChoices, yearChoices ->
            (listOf(clearFilters) + artistChoices.distinct() + albumChoices.distinct() + yearChoices.distinct())
        }
    }

    fun getFilterArtistChoices(): Flow<List<String>> {
        val filterArtistChoices = repository.getFilterArtistChoices()
        return combine(MutableStateFlow("Back"), filterArtistChoices) { goBack, artistChoices ->
            (listOf(goBack) + artistChoices.distinct())
        }
    }

    fun getFilterAlbumChoices(): Flow<List<String>> {
        val filterAlbumChoices = repository.getFilterAlbumChoices()
        return combine(MutableStateFlow("Back"), filterAlbumChoices) { goBack, albumChoices ->
            (listOf(goBack) + albumChoices.distinct())
        }
    }
    fun getFilterYearChoices(): Flow<List<String>> {
        val filterYearChoices = repository.getFilterYearChoices()
        return combine(MutableStateFlow("Back"), filterYearChoices) { goBack, yearChoices ->
            (listOf(goBack) + yearChoices.distinct())
        }
    }

    fun insert(historyItem: HistoryItem) = viewModelScope.launch(Dispatchers.IO) { repository.insert(historyItem) }

    fun delete(historyItem: HistoryItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(historyItem)
        filterSortSearch()
    }

    fun like(historyItem: HistoryItem, context: Context, userId: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.like(historyItem)

        val prefs = context.getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)
        val savedPlaylistId = prefs.getString("playlist_id_$userId", null)

        val songName = historyItem.title
        val artistName = historyItem.artists
        val albumName = historyItem.album

        Log.d("LikedSong", "Song Name = $songName Album Name = $albumName")
        Log.d("LikedSong", "Playlist = $savedPlaylistId")

        savedPlaylistId?.let { playlistId ->
            SpotifyPlaylists.addTrackToPlaylist(
                context = context,
                playlistId = playlistId,
                songTitle = songName,
                artistName = artistName,
                albumName = albumName
            )
        }

        SpotifyLikeSong.likeSong(context = context, songName = songName, artistName = artistName, albumName = albumName)
        filterSortSearch()
    }
    fun unlike(historyItem: HistoryItem, context: Context, userId: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.unlike(historyItem)

        val prefs = context.getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)
        val savedPlaylistId = prefs.getString("playlist_id_$userId", null)

        val songName = historyItem.title
        val artistName = historyItem.artists
        val albumName = historyItem.album
        Log.d("UnlikedSong", "Song Name = $songName Album Name = $albumName")
        Log.d("UnlikedSong", "Playlist = $savedPlaylistId")
        savedPlaylistId?.let { playlistId ->
            SpotifyPlaylists.removeTrackFromPlaylist(
                context = context,
                playlistId = playlistId,
                songTitle = songName,
                artistName = artistName,
                albumName = albumName)
        }
        filterSortSearch()
        SpotifyLikeSong.unlikeSong(context = context, songName = songName, artistName = artistName, albumName = albumName)
    }
}


data class FilterSortSearchChoices(
    var query: String,
    var filters: List<List<String?>>,
    var sortChoice: String?,
    val isAscending: Boolean
)