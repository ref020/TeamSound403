package com.alexmercerind.audire

import android.util.Log
import com.alexmercerind.audire.models.Music // Make sure this is imported
import com.alexmercerind.audire.repository.ShazamIdentifyRepository
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    private suspend fun identifySong(audioData: ByteArray) {
        Log.d("DataLayerService", "Calling repository to identify song.")
        try {
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
