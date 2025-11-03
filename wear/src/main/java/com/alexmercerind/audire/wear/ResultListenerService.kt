package com.alexmercerind.audire.wear

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

// Path for receiving the result from the phone. Must match the phone app.
const val RESULT_DATA_PATH = "/result-data"

class ResultListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == RESULT_DATA_PATH) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                Log.d("ResultListenerService", "Result received from phone.")

                // Create an Intent to broadcast the result to the active Activity
                val intent = Intent("com.alexmercerind.audire.wear.SONG_RESULT").apply {
                    // *** FIX: Restrict the broadcast to your app's package for security ***
                    `package` = applicationContext.packageName

                    putExtra("found", dataMap.getBoolean("found"))
                    if (dataMap.getBoolean("found")) {
                        putExtra("title", dataMap.getString("title"))
                        putExtra("artist", dataMap.getString("artist"))
                    }
                }
                // *** FIX: Use the standard sendBroadcast instead of the deprecated one ***
                sendBroadcast(intent)
            }
        }
    }
}