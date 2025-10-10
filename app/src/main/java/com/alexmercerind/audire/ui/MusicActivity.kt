package com.alexmercerind.audire.ui

import android.app.SearchManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import coil.ImageLoader
import coil.load
import coil.request.CachePolicy
import com.alexmercerind.audire.R
import com.alexmercerind.audire.databinding.ActivityMusicBinding
import com.alexmercerind.audire.mappers.toSearchQuery
import com.alexmercerind.audire.models.Music
import com.google.android.material.snackbar.Snackbar
import com.alexmercerind.audire.api.genius.GeniusApi
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.alexmercerind.audire.services.LyricsScraper
import com.alexmercerind.audire.services.LyricsFormatter
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import com.google.android.material.button.MaterialButton

class MusicActivity : AppCompatActivity() {
    companion object {
        const val MUSIC = "MUSIC"
        const val SPOTIFY_PACKAGE_NAME = "com.spotify.music"
        const val YOUTUBE_PACKAGE_NAME = "com.google.android.youtube"
    }

    private lateinit var music: Music
    private lateinit var binding: ActivityMusicBinding
    private lateinit var viewModel: IdentifyViewModel

    private val LyricsScraper = LyricsScraper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        binding = ActivityMusicBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        music = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(MUSIC) as Music
        } else {
            intent.getSerializableExtra(MUSIC, Music::class.java)!!
        }

        binding.titleTextView.text = music.title
        binding.artistsTextView.text = music.artists

        if (music.year != null) {
            binding.yearChip.text = " ${music.year}"
        } else {
            binding.yearChip.visibility = View.GONE
        }
        if (music.album != null) {
            binding.albumChip.text = " ${music.album}"
        } else {
            binding.albumChip.visibility = View.GONE
        }
        if (music.year != null) {
            binding.labelChip.text = " ${music.label}"
        } else {
            binding.labelChip.visibility = View.GONE
        }

        viewModel = androidx.lifecycle.ViewModelProvider(this)[IdentifyViewModel::class.java]
        viewModel.fetchRelatedSongs(music)
        Log.d("GeniusUi", "Starting collection of relatedSongs")
        viewModel.relatedSongs
            .onEach { songs ->
                if (songs.isNotEmpty()) {
                    binding.geniusSongsTitle.visibility = View.VISIBLE
                    binding.geniusSongsContainer.visibility = View.VISIBLE

                    // Clear old ones
                    binding.geniusSongsContainer.removeAllViews()

                    songs.forEach { song ->
                        val button = MaterialButton(this).apply {
                            text = song
                            setIconResource(R.drawable.spotify) // optional
                            iconPadding = 16
                            setOnClickListener {
                                try {
                                    val intent = Intent(Intent.ACTION_MAIN).apply {
                                        action = MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH
                                        component = ComponentName(
                                            SPOTIFY_PACKAGE_NAME,
                                            "$SPOTIFY_PACKAGE_NAME.MainActivity"
                                        )
                                        putExtra(SearchManager.QUERY, song)
                                    }
                                    startActivity(intent)
                                } catch (e: Throwable) {
                                    showFailureSnackbar()
                                    e.printStackTrace()
                                }
                            }
                        }
                        binding.geniusSongsContainer.addView(button)
                    }
                } else {
                    binding.geniusSongsTitle.visibility = View.GONE
                    binding.geniusSongsContainer.visibility = View.GONE
                }
            }
            .launchIn(lifecycleScope)



        //uses coroutines to run the function in the background
        lifecycleScope.launch {
            val songLyricURL = withContext(Dispatchers.IO) {
                GeniusApi.search("${music.title} ${music.artists}") // retreive metadata from Genius
            }

            val songLyric = withContext(Dispatchers.IO) {
                LyricsScraper.getLyricsFromGenius(songLyricURL) // retreive lyrics from accessed Metadata
            }

            //print debug in logcat to display variable songLyrics
            Log.d("lyrics",songLyric)

            //display a title for the lyrics section
            //display the lyrics in a text view
            binding.lyricsTitleTextView.visibility = View.VISIBLE
            binding.lyricsBodyTextView.visibility = View.VISIBLE
            binding.lyricsBodyTextView.text = " ${songLyric}"
        }

        binding.coverImageView.load(
            music.cover,
            ImageLoader.Builder(this)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        ) {
            crossfade(true)
        }

        binding.searchMaterialButton.setOnClickListener {
            try {
                val uri = Uri.parse("https://www.duckduckgo.com/?q=${music.toSearchQuery()}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } catch (e: Throwable) {
                showFailureSnackbar()
                e.printStackTrace()
            }
        }
        binding.spotifyMaterialButton.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    action = MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH
                    component =
                        ComponentName(SPOTIFY_PACKAGE_NAME, "$SPOTIFY_PACKAGE_NAME.MainActivity")
                    putExtra(SearchManager.QUERY, music.toSearchQuery())
                }
                startActivity(intent)
            } catch (e: Throwable) {
                showFailureSnackbar()
                e.printStackTrace()
            }
        }
        binding.youtubeMaterialButton.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage(YOUTUBE_PACKAGE_NAME)
                    putExtra(SearchManager.QUERY, music.toSearchQuery())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: Throwable) {
                showFailureSnackbar()
                e.printStackTrace()
            }
        }

        binding.downloadMaterialButton.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage(YOUTUBE_PACKAGE_NAME)
                    putExtra(SearchManager.QUERY, music.toSearchQuery())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: Throwable) {
                showFailureSnackbar()
                e.printStackTrace()
            }
        }

    }

    private fun showFailureSnackbar() {
        Snackbar.make(binding.root, R.string.action_view_error, Snackbar.LENGTH_LONG).show()
    }
}
