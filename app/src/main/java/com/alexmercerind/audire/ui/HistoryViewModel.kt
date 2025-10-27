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

import android.util.Log
import com.alexmercerind.audire.api.Spotify.SpotifyPlaylists
import android.content.Context

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    val historyItems: StateFlow<List<HistoryItem>?>
        get() = _historyItems

    private val _historyItems = MutableStateFlow<List<HistoryItem>?>(null)

    var query: String = ""
        set(value) {
            // Avoid duplicate operation in Room.
            if (value == field) {
                return
            }

            field = value
            viewModelScope.launch {
                mutex.withLock {
                    _historyItems.emit(
                        when (value) {
                            // Search query == "" -> Show all HistoryItem(s)
                            "" -> getAll().first()
                            // Search query != "" -> Show search HistoryItem(s)
                            else -> search(value.lowercase())
                        }
                    )
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
