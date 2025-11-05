package com.alexmercerind.audire.wear // Corrected package name to match folder structure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Added missing import for LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.gms.wearable.Wearable // Added missing import for Wearable
import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.android.gms.wearable.PutDataMapRequest
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFormat
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.max

// Communication path constants
const val START_RECOGNITION_PATH = "/start-recognition"
const val OPEN_ON_PHONE_PATH = "/open-on-phone"
const val AUDIO_DATA_PATH = "/audio-data"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudireWearApp()
        }
    }
}

// These are our two routes, or screen identifiers
const val RECOGNITION_ROUTE = "recognition"
const val RESULT_ROUTE = "result"

var isLoading = mutableStateOf(false)
const val SONG_NOT_FOUND_TITLE = "Song Not Found"
const val SONG_NOT_FOUND_ARTIST = "Please try again"

@Composable
fun WaveAnimation(waveColor: Color) { // <--- 1. Accept a Color parameter
    val waves = listOf(0.0f, 0.5f, 1.0f) // Start delays for each wave

    val transition = rememberInfiniteTransition(label = "WaveTransition")

    val animatedValues = waves.map { delay ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset((delay * 2000).toInt())
            ),
            label = "WaveAnimationValue"
        )
    }

    Box(contentAlignment = Alignment.Center) {
        // The center microphone icon
        Icon(
            painter = painterResource(id = R.drawable.baseline_mic_24),
            contentDescription = "Listening...",
            modifier = Modifier.size(ButtonDefaults.LargeButtonSize),
            tint = waveColor // <--- 2. Use the passed-in color
        )

        // The animating waves
        animatedValues.forEach { anim ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2 * anim.value
                val alpha = 1f - anim.value

                drawCircle(
                    color = waveColor, // <--- 3. Use the passed-in color
                    radius = radius,
                    alpha = alpha,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }
    }
}


@Composable
fun AudireWearApp() {
    AudireTheme {
        val navController = rememberSwipeDismissableNavController()
        val context = LocalContext.current

        // Listen for results from the ResultListenerService
        ListenForSongResult(context = context, navController = navController)

        SwipeDismissableNavHost(
            navController = navController,
            startDestination = RECOGNITION_ROUTE
        ) {
            composable(RECOGNITION_ROUTE) {
                RecognitionScreen(
                    isLoading = isLoading.value,
                    // *** FIX: Provide the lambda to change the state ***
                    onIsLoadingChange = { isLoading.value = it },
                    onStartRecognition = { audioData ->
                        // This lambda now correctly only handles sending data
                        Log.d("WearApp", "Audio recorded. Size: ${audioData.size}. Sending to phone...")
                        val putDataMapReq = PutDataMapRequest.create(AUDIO_DATA_PATH)
                        putDataMapReq.dataMap.putByteArray("audio_pcm", audioData)
                        putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis())
                        val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
                        Wearable.getDataClient(context).putDataItem(putDataReq)
                    }
                )
            }

            composable(
                route = "$RESULT_ROUTE/{title}/{artist}",
                arguments = listOf(
                    navArgument("title") { defaultValue = "" },
                    navArgument("artist") { defaultValue = "" }
                )
            ) { backStackEntry ->
                ResultScreen(
                    navController = navController, // <-- Pass the NavController
                    title = backStackEntry.arguments?.getString("title") ?: "",
                    artist = backStackEntry.arguments?.getString("artist") ?: ""
                )
            }

        }
    }
}

// New Composable to handle the BroadcastReceiver lifecycle
@Composable
fun ListenForSongResult(context: Context, navController: NavHostController) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isLoading.value = false // Stop loading
                val found = intent.getBooleanExtra("found", false)
                val title = if (found) intent.getStringExtra("title") else SONG_NOT_FOUND_TITLE
                val artist = if (found) intent.getStringExtra("artist") else SONG_NOT_FOUND_ARTIST

                // Navigate to the result screen, ensuring it's properly encoded for the route
                navController.navigate("$RESULT_ROUTE/${Uri.encode(title)}/${Uri.encode(artist)}")
            }
        }
        val filter = IntentFilter("com.alexmercerind.audire.wear.SONG_RESULT")

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Clean up the receiver when the composable is disposed
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}

// *** FIX: Update the function signature ***
@Composable
fun RecognitionScreen(
    isLoading: Boolean,
    onIsLoadingChange: (Boolean) -> Unit,
    onStartRecognition: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasPermission = isGranted
    }
    val audioRecorder = remember { AudioRecorder(context) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            WaveAnimation(waveColor = MaterialTheme.colors.primary)
        } else {
            // --- Wrap buttons in a Column for vertical arrangement ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // --- Main Recognition Button ---
                Button(
                    onClick = {
                        if (hasPermission) {
                            onIsLoadingChange(true)
                            audioRecorder.startRecording { audioData ->
                                onStartRecognition(audioData)
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.size(ButtonDefaults.LargeButtonSize)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_mic_24),
                        contentDescription = "Start Recognition"
                    )
                }

                // --- ADD THE SPACER HERE ---
                Spacer(Modifier.height(10.dp))

                // --- Ping Test Button ---
                Button(
                    onClick = {
                        Log.d("WearApp", "Ping button clicked. Sending message to phone...")
                        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                            nodes.firstOrNull()?.let { node ->
                                Wearable.getMessageClient(context).sendMessage(
                                    node.id,
                                    "/ping-test",
                                    "PING".toByteArray()
                                ).apply {
                                    addOnSuccessListener { Log.d("WearApp", "Ping message sent successfully.") }
                                    addOnFailureListener { e -> Log.e("WearApp", "Ping message failed.", e) }
                                }
                            } ?: Log.e("WearApp", "No connected node to ping.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)
                ) {
                    Text("Ping Test")
                }
            }
        }
    }
}


// --- Composable for the Result Screen ---
@Composable
fun ResultScreen(navController: NavHostController, title: String, artist: String) {
    val isSongFound = title != SONG_NOT_FOUND_TITLE
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, textAlign = TextAlign.Center, style = MaterialTheme.typography.title3)
        Spacer(Modifier.height(4.dp))
        Text(text = artist, style = MaterialTheme.typography.body2, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))

        if (isSongFound) {
            // --- Song was found: Show "Open on phone" button ---
            Button(
                onClick = {
                    // Find the connected device and send a message to open the app
                    Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                        nodes.firstOrNull()?.id?.also { nodeId ->
                            val data = "$title|$artist".toByteArray()
                            Wearable.getMessageClient(context).sendMessage(nodeId, OPEN_ON_PHONE_PATH, data)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "Open on phone"
                )
            }
        } else {
            // --- Song not found: Show "Retry" button ---
            Button(
                onClick = {
                    // Navigate back to the recognition screen
                    navController.popBackStack(RECOGNITION_ROUTE, inclusive = false)
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_retry), // Assuming you have a retry icon
                    contentDescription = "Retry"
                )
            }
        }
    }
}


// --- ADDED AUDIO RECORDER CLASS ---

class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var isRecording = false

    fun startRecording(onComplete: (ByteArray) -> Unit) {
        if (isRecording) {
            return
        }
        isRecording = true

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        outputFile = File.createTempFile("audio_record", ".mp3", context.cacheDir)

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) // Using MPEG_4 container
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)    // AAC is a good compressed format
            setAudioSamplingRate(SAMPLE_RATE)
            setAudioEncodingBitRate(SAMPLE_RATE * CHANNEL_COUNT * 16) // Bitrate = Sample Rate * Channels * Bits per Sample
            setAudioChannels(CHANNEL_COUNT)


            setOutputFile(outputFile?.absolutePath)
            setMaxDuration(7_000) // Let's use 7 seconds to match the phone app's likely expectation

            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    stopRecording(onComplete)
                }
            }

            try {
                prepare()
                start()
            } catch (e: IOException) {
                isRecording = false
            }
        }
    }

    private fun stopRecording(onComplete: (ByteArray) -> Unit) {
        if (!isRecording) return

        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                // Ignore exceptions on stop
            }
        }
        mediaRecorder = null

        coroutineScope.launch {
            val audioData = outputFile?.readBytes()
            withContext(Dispatchers.Main) {
                if (audioData != null) {
                    onComplete(audioData)
                }
                outputFile?.delete()
                isRecording = false
            }
        }
    }

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHANNEL_COUNT = 1
        private const val SAMPLE_WIDTH = 2

    }
}
