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
import android.content.Context

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    val historyItems: StateFlow<List<HistoryItem>?>
        get() = _historyItems

    private val _historyItems = MutableStateFlow<List<HistoryItem>?>(null)

    //var filterChoices1: List<String> = listOf()
    var filterType = MutableStateFlow<String?>(null)
    var filterChoice = MutableStateFlow<String?>(null)
    var sortChoice = MutableStateFlow<String?>(null)
    var isAscending = MutableStateFlow<Boolean>(true)
    var query = MutableStateFlow<String>("")
//        set(value) {
//            // Avoid duplicate operation in Room.
//            if (value == field) {
//                return
//            }
//
//            field = value
//            viewModelScope.launch {
//                mutex.withLock {
//
//                    _historyItems.emit(
//                        when (value) {
//                            // Search query == "" -> Show all HistoryItem(s)
//                            "" -> when (value) {
//                                    null -> getAll().first()
//                                    else -> getAll().first()
//                                }
//                            // Search query != "" -> Show search HistoryItem(s)
//                            else -> search(value.lowercase())
//                        }
//                    )
//                }
//            }

    fun filterSortSearch() {
        viewModelScope.launch {
            combine(
                query,
                filterType,
                filterChoice,
                sortChoice,
                isAscending
            ) { query, filterType, filterChoice, sortChoice, isAscending ->
                FilterSortSearchChoices(query, filterType, filterChoice, sortChoice, isAscending)
            }.collect { choices ->
                mutex.withLock {
                    val searchItems: List<HistoryItem> = when (choices.query) {
                        "" -> getAll().first()
                        else -> search(choices.query.lowercase())
                    }
                    val filteredItems: List<HistoryItem> = when (choices.filterType) {
                        "artist" -> searchItems.filter { it.artists == choices.filterChoice }
                        "year" -> searchItems.filter { it.year == choices.filterChoice }
                        else -> searchItems
                    }
                    var sortedItems: List<HistoryItem> = when (choices.sortChoice) {
                        "Date Added" -> filteredItems.sortedBy { it.timestamp }
                        "Title" -> filteredItems.sortedBy { it.title }
                        "Year" -> filteredItems.sortedBy { it.year }
                        "Artist" -> filteredItems.sortedBy { it.artists }
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
        val filterYearChoices = getFilterYearChoices()
        return combine(MutableStateFlow("No Filter"), filterArtistChoices, filterYearChoices) { noFilter, artistChoices, yearChoices ->
            (listOf(noFilter) + artistChoices + yearChoices).distinct()
        }
    }

    fun getFilterArtistChoices(): Flow<List<String>> {
        return repository.getFilterArtistChoices()
    }
    fun getFilterYearChoices(): Flow<List<String>> {
        return repository.getFilterYearChoices()
    }

    fun insert(historyItem: HistoryItem) = viewModelScope.launch(Dispatchers.IO) { repository.insert(historyItem) }

    fun delete(historyItem: HistoryItem) = viewModelScope.launch(Dispatchers.IO) { repository.delete(historyItem) }

    val prefs = getApplication<Application>().getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE)
    val savedPlaylistId = prefs.getString("playlist_id", null)
    fun like(historyItem: HistoryItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.like(historyItem)
        val songName = historyItem.title
        val artistName = historyItem.artists
        val albumName = historyItem.album
        Log.d("LikedSong", "Song Name = $songName Album Name = $albumName")
        Log.d("LikedSong", "Playlist = $savedPlaylistId")
        savedPlaylistId?.let { playlistId ->
            SpotifyPlaylists.addTrackToPlaylist(
                playlistId = playlistId,
                songTitle = songName,
                artistName = artistName,
                albumName = albumName)
        }
    }
    fun unlike(historyItem: HistoryItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.unlike(historyItem)
        val songName = historyItem.title
        val artistName = historyItem.artists
        val albumName = historyItem.album
        Log.d("UnlikedSong", "Song Name = $songName Album Name = $albumName")
        Log.d("UnlikedSong", "Playlist = $savedPlaylistId")
        savedPlaylistId?.let { playlistId ->
            SpotifyPlaylists.removeTrackFromPlaylist(
                playlistId = playlistId,
                songTitle = songName,
                artistName = artistName,
                albumName = albumName)
        }
    }
}


data class FilterSortSearchChoices(
    var query: String,
    var filterType: String?,
    var filterChoice: String?,
    var sortChoice: String?,
    val isAscending: Boolean
)