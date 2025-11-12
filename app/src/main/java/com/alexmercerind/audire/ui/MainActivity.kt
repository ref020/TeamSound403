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
import android.content.Context
import com.alexmercerind.audire.api.Spotify.SpotifyAuth
import android.content.Intent

import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val accessToken = SpotifyAuth.getAccessToken(this)
        val refreshToken = SpotifyAuth.getRefreshToken(this)

        if (accessToken == null || refreshToken == null) {
            startActivity(Intent(this, SpotifyLoginActivity::class.java))
            finish()
            return
        }


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

            // Get current Spotify user
            val userProfile = SpotifyAuth.getCurrentUserProfile(this@MainActivity)
            val userId = userProfile?.getString("id") ?: return@launch

            val key = "playlist_id_$userId"
            val savedPlaylistId = prefs.getString(key, null)
            var playlist: JSONObject? = null

            if (savedPlaylistId != null) {
                val exists = SpotifyPlaylists.doesPlaylistExist(this@MainActivity, savedPlaylistId)
                if (exists) {
                    Log.d("SpotifyTest", "Using Playlist: $savedPlaylistId")
                    return@launch
                } else {
                    Log.d("SpotifyTest", "Saved playlist $savedPlaylistId was deleted. Recreating...")
                }
            }

            playlist = SpotifyPlaylists.createPlaylist(this@MainActivity, "Audire Songs")
            playlist?.let {
                val playlistId = it.getString("id")
                prefs.edit().putString(key, playlistId).apply()
                Log.d("SpotifyTest", "Created or found playlist: $playlistId")
            }
        }
    }
}
