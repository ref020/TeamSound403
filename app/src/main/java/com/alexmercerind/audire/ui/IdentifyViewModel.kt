package com.alexmercerind.audire.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexmercerind.audire.models.Music
import com.alexmercerind.audire.repository.ShazamIdentifyRepository
import com.alexmercerind.audire.utils.AudioRecorder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log

class IdentifyViewModel : ViewModel() {
    val error get() = _error.asSharedFlow()
    val music get() = _music.asSharedFlow()
    val active get() = audioRecorder.active
    val duration get() = audioRecorder.duration

    private val _relatedSongs = MutableStateFlow<List<String>>(emptyList())
    val relatedSongs = _relatedSongs.asStateFlow()

    private val _error = MutableSharedFlow<Unit>()
    private val _music = MutableSharedFlow<Music>()

    private val audioRecorder = AudioRecorder(viewModelScope)
    private val repository = ShazamIdentifyRepository()

    fun start() {
        audioRecorder.start()
    }

    fun stop() {
        audioRecorder.stop()
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stop()
    }

    init {
        combine(audioRecorder.duration, audioRecorder.buffer) { duration, buffer ->
            duration to buffer
        }
            .sampleImmediate(2000L)
            .onEach { (duration, buffer) ->
                runCatching {
                    Log.d("IdentifyDebug", "checking duration=$duration, buffer size=${buffer.size}")
                    if (buffer.isEmpty()) return@onEach
                    if (duration < MIN_DURATION) return@onEach
                    if (duration > MAX_DURATION) {
                        _error.emit(Unit)
                        audioRecorder.stop()
                        return@onEach
                    }
                    val result = repository.identify(duration, buffer)
                    Log.d("ResultDebug", "Identify result = $result")
                    repository.identify(duration, buffer)?.let {

                        if (it.album.isNullOrEmpty() && duration < MAX_DURATION) return@onEach
                        _music.emit(it)
                        fetchRelatedSongs(it)
                        audioRecorder.stop()
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun fetchRelatedSongs(music: Music) {
        viewModelScope.launch {

            runCatching {
                val songs = repository.getSongsByArtist(music.artists)
                Log.d("GeniusDebug", "Artist=${music.artists}, Songs=$songs")
                if (songs.isNotEmpty()) {
                    Log.d("GeniusEmit", "About to emit ${songs.size} songs")
                    _relatedSongs.value = songs.toList()
                    Log.d("GeniusEmit", "Related songs set: $songs")
                } else {
                    Log.d("GeniusEmit", "No songs found for artist ${music.artists}")
                }
            }.onFailure {
                Log.e("GeniusDebug", "Error fetching songs", it)
            }
        }
    }

    private fun <T> Flow<T>.sampleImmediate(periodMillis: Long): Flow<T> = channelFlow {
        var lastEmitTime = 0L
        collect { value ->
            val now = System.currentTimeMillis()
            if (lastEmitTime == 0L || now - lastEmitTime >= periodMillis) {
                lastEmitTime = now
                trySend(value)
            }
        }
    }

    companion object {
        const val MIN_DURATION = 3
        const val MAX_DURATION = 12
    }
}
