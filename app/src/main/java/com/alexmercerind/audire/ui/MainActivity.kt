package com.alexmercerind.audire.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.alexmercerind.audire.R
import com.alexmercerind.audire.api.Spotify.SpotifyPlaylists
import com.alexmercerind.audire.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.getBooleanExtra("from_widget", false) == true) {
            // Navigate to IdentifyFragment (or wherever IdentifyViewModel lives)
            supportFragmentManager.setFragmentResult("widget_identify", Bundle())
        }

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        // https://stackoverflow.com/a/50537193/12825435
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.content) as NavHostFragment
        binding.bottomNavigationView.setupWithNavController(navHostFragment.navController)

        lifecycleScope.launch {
            val prefs = getSharedPreferences("spotify_prefs", MODE_PRIVATE)
            val savedPlaylistId = prefs.getString("playlist_id", null)
            var playlist: JSONObject? = null

            if (savedPlaylistId != null) {
                val exists = SpotifyPlaylists.doesPlaylistExist(savedPlaylistId)
                if (exists) {
                    Log.d("SpotifyTest", "Using Playlist: $savedPlaylistId")
                    return@launch
                }
                else {
                    Log.d("SpotifyTest", "Saved playlist was deleted. Recreating...")
                }
            }
            playlist = SpotifyPlaylists.createPlaylist("Audire Songs")
            playlist?.let {
                val playlistId = it.getString("id")
                prefs.edit().putString("playlist_id", playlistId).apply()
                Log.d("SpotifyTest", "Created or found playlist: $playlistId")
            }
        }
    }
}
