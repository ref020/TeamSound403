package com.alexmercerind.audire.ui

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.alexmercerind.audire.api.Spotify.SpotifyAuth
import kotlinx.coroutines.launch
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.content.Context
import android.util.Log
import com.alexmercerind.audire.R
import com.alexmercerind.audire.api.Spotify.SpotifyPlaylists
import org.json.JSONObject

class SpotifyLoginActivity : AppCompatActivity() {

    private val clientId = "658c017dff584b91b1b60c4da4798234"
    private val redirectUri = "audire://spotify-auth"
    private val scopes = "user-library-read user-library-modify user-read-email user-read-private playlist-modify-public playlist-modify-private"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_spotify_login)

        val loginButton: Button = findViewById(R.id.loginButton)
        loginButton.setOnClickListener {
            startSpotifyLogin()
        }

        // Handle Spotify redirect
        handleSpotifyRedirect(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSpotifyRedirect(intent)

        lifecycleScope.launch {
            val prefs = getSharedPreferences("spotify_prefs", MODE_PRIVATE)

            // Get current Spotify user
            val userProfile = SpotifyAuth.getCurrentUserProfile(this@SpotifyLoginActivity)
            val userId = userProfile?.getString("id") ?: return@launch

            val key = "playlist_id_$userId"
            val savedPlaylistId = prefs.getString(key, null)
            var playlist: JSONObject? = null

            if (savedPlaylistId != null) {
                val exists = SpotifyPlaylists.doesPlaylistExist(this@SpotifyLoginActivity, savedPlaylistId)
                if (exists) {
                    Log.d("SpotifyTest", "Using Playlist: $savedPlaylistId")
                    return@launch
                } else {
                    Log.d("SpotifyTest", "Saved playlist $savedPlaylistId was deleted. Recreating...")
                }
            }

            playlist = SpotifyPlaylists.createPlaylist(this@SpotifyLoginActivity, "Audire Songs")
            playlist?.let {
                val playlistId = it.getString("id")
                prefs.edit().putString(key, playlistId).apply()
                Log.d("SpotifyTest", "Created or found playlist: $playlistId")
            }
        }

    }

    private fun startSpotifyLogin() {
        val loginUrl = "https://accounts.spotify.com/authorize" +
                "?client_id=$clientId" +
                "&response_type=code" +
                "&redirect_uri=${Uri.encode(redirectUri)}" +
                "&scope=${Uri.encode(scopes)}" +
                "&show_dialog=true"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(loginUrl))
        startActivity(intent)
    }

    private fun getEncryptedPrefs(context: Context) =
        EncryptedSharedPreferences.create(
            context,
            "spotify_tokens",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    private fun saveTokens(accessToken: String, refreshToken: String) {
        val prefs = getEncryptedPrefs(this)
        prefs.edit().apply {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            apply()
        }
    }

    private fun handleSpotifyRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.toString().startsWith(redirectUri)) {
            val code = uri.getQueryParameter("code") ?: return
            lifecycleScope.launch {
                val (accessToken, refreshToken) =
                    SpotifyAuth.exchangeCodeForTokens(code) ?: return@launch
                saveTokens(accessToken, refreshToken)

                val mainIntent = Intent(this@SpotifyLoginActivity, MainActivity::class.java)
                startActivity(mainIntent)
                finish()
            }
        }
    }
}
