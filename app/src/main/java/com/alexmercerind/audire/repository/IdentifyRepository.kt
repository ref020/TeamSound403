package com.alexmercerind.audire.repository

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.alexmercerind.audire.models.Music
import com.alexmercerind.audire.ui.IdentifyViewModel.Companion.MAX_DURATION
import com.alexmercerind.audire.ui.IdentifyViewModel.Companion.MIN_DURATION
import com.alexmercerind.audire.utils.AudioRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach



abstract class IdentifyRepository {
    abstract suspend fun identify(duration: Int, data: ByteArray): Music?

}
