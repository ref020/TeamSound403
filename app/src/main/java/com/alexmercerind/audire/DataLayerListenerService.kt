package com.alexmercerind.audire

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.alexmercerind.audire.models.Music // Make sure this is imported
import com.alexmercerind.audire.repository.ShazamIdentifyRepository
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.io.output.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.media.*
import android.os.ParcelFileDescriptor
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

// Path for receiving audio from the watch
const val AUDIO_DATA_PATH = "/audio-data"
// New path for sending the result back to the watch
const val RESULT_DATA_PATH = "/result-data"

class DataLayerListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val repository = ShazamIdentifyRepository()
    private val dataClient by lazy { Wearable.getDataClient(this) }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        Log.d("DataFLow", "onDataChanged triggered on PHONE device")

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == AUDIO_DATA_PATH) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val audioData = dataMap.getByteArray("audio_pcm")

                if (audioData != null) {
                    Log.d("DataLayerService", "Received audio data of size: ${audioData.size}")
                    serviceScope.launch {
                        identifySong(audioData)
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        // Check if the message path matches our ping test path
        if (messageEvent.path == "/ping-test") {
            // THIS IS THE DEBUG STATEMENT:
            Log.d("DataLayerService", "Ping received from watch!")
        }
    }

    private suspend fun identifySong(audioData: ByteArray) {
        Log.d("DataLayerService", "Calling repository to identify song.")
        try {
            FileConverter.convertToPCM(applicationContext, audioData)
            Log.d("ShazamIdentifyRepo", "Using WAV file: ${audioData.size} bytes")
            val musicResult = repository.identifyFromAudioData(audioData)
            sendResultToWatch(musicResult)
        } catch (e: Exception) {
            Log.e("DataLayerService", "Error during identification", e)
            sendResultToWatch(null) // Send a failure/not-found result
        }
    }

    /**
     * Sends the identification result (or a failure state) back to the watch
     * using the DataClient.
     */
    private suspend fun sendResultToWatch(music: Music?) {
        try {
            val putDataMapReq = PutDataMapRequest.create(RESULT_DATA_PATH).apply {
                if (music != null) {
                    // --- Song Found ---
                    dataMap.putBoolean("found", true)
                    dataMap.putString("title", music.title)
                    dataMap.putString("artist", music.artists)
                    Log.d("DataLayerService", "Sending SUCCESS to watch: ${music.title}")
                } else {
                    // --- Song Not Found ---
                    dataMap.putBoolean("found", false)
                    Log.d("DataLayerService", "Sending NOT FOUND to watch.")
                }
                // Add a timestamp to ensure the data item always triggers onDataChanged
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }

            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putDataReq).await()
            Log.d("DataLayerService", "Result data sent successfully.")
        } catch (e: Exception) {
            Log.e("DataLayerService", "Failed to send result to watch", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}


//Helper function and object
object FileConverter {

    fun convertToPCM(context: Context, audioData: ByteArray): File? {
        return try {
            val outputFile = File(context.cacheDir, "converted_audio_${System.currentTimeMillis()}.wav")

            FileOutputStream(outputFile).use { output ->
                // Write a simple WAV header + data
                writeWavHeader(output, audioData.size)
                output.write(audioData)
            }

            Log.d("FileConverter", "WAV file written: ${outputFile.absolutePath}")
            outputFile
        } catch (e: Exception) {
            Log.e("FileConverter", "PCM conversion failed", e)
            null
        }
    }

    private fun writeWavHeader(out: FileOutputStream, dataLength: Int) {
        val sampleRate = 16000          // Match your recording sample rate
        val numChannels = 1             // Mono
        val bitsPerSample = 16

        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val totalDataLen = dataLength + 36

        val header = ByteArray(44)

        // RIFF/WAVE header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        writeInt(header, 4, totalDataLen)
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        writeInt(header, 16, 16) // Subchunk1Size (16 for PCM)
        writeShort(header, 20, 1.toShort()) // PCM format
        writeShort(header, 22, numChannels.toShort())
        writeInt(header, 24, sampleRate)
        writeInt(header, 28, byteRate)
        writeShort(header, 32, (numChannels * bitsPerSample / 8).toShort())
        writeShort(header, 34, bitsPerSample.toShort())
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        writeInt(header, 40, dataLength)

        out.write(header, 0, 44)
    }

    private fun writeInt(header: ByteArray, offset: Int, value: Int) {
        header[offset] = (value and 0xff).toByte()
        header[offset + 1] = ((value shr 8) and 0xff).toByte()
        header[offset + 2] = ((value shr 16) and 0xff).toByte()
        header[offset + 3] = ((value shr 24) and 0xff).toByte()
    }

    private fun writeShort(header: ByteArray, offset: Int, value: Short) {
        header[offset] = (value.toInt() and 0xff).toByte()
        header[offset + 1] = ((value.toInt() shr 8) and 0xff).toByte()
    }
}


