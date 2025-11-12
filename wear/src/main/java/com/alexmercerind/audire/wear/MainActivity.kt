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
import android.media.AudioRecord
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.max
import java.io.ByteArrayOutputStream

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
    private var audioRecord: AudioRecord? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var isRecording = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(onComplete: (ByteArray) -> Unit) {
        if (isRecording) return
        isRecording = true

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        val audioBuffer = ByteArray(bufferSize)
        val outputStream = ByteArrayOutputStream()

        audioRecord?.startRecording()

        coroutineScope.launch {
            val startTime = System.currentTimeMillis()
            while (isRecording && System.currentTimeMillis() - startTime < MAX_DURATION_MS) {
                val read = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                if (read > 0) outputStream.write(audioBuffer, 0, read)
            }
            stopRecording(onComplete, outputStream.toByteArray())
        }
    }

    fun stopRecording(onComplete: (ByteArray) -> Unit, data: ByteArray? = null) {
        if (!isRecording) return
        isRecording = false

        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null

        coroutineScope.launch(Dispatchers.Main) {
            if (data != null) onComplete(data)
        }
    }

    companion object {
        // Adjusted for <100 KB total
        private const val SAMPLE_RATE = 8000        // 8 kHz
        private const val MAX_DURATION_MS = 5000L   // 3 seco
    }
}