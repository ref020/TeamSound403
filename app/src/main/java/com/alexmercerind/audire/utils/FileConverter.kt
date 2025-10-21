package com.alexmercerind.audire.utils
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


import kotlinx.coroutines.flow.MutableStateFlow



class FileConverter() {

    private var job: Job? = null

    val active get() = _active.asStateFlow()

    val duration get() = _duration.asStateFlow()

    private val _active = MutableStateFlow(false)

    private val _duration = MutableStateFlow(0)
    private val mutex = Mutex()
    var isAudioTrack: Boolean = false
    val extractor = MediaExtractor()
    var trackIndex = -1

    val pcmOutput = ByteArrayOutputStream()

//    @Throws(SecurityException::class)
//    fun start() {
//        scope.launch {
//            mutex.withLock {
//                if (_active.value) return@launch
//                job = scope.launch(Dispatchers.IO) { readFile(context, inputFileUri) }
//            }
//        }
//    }

    fun readFile(context: Context?, inputFileUri: Uri): ByteArray {
        val pfd = context!!.contentResolver.openFileDescriptor(inputFileUri, "r")
            ?: throw IllegalArgumentException("Cannot open URI: $inputFileUri")

        extractor.setDataSource(pfd.fileDescriptor)

        val numTracks = extractor.getTrackCount()
            for (i in 0 until numTracks) {
            val audioFormat: MediaFormat = extractor.getTrackFormat(i)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME).toString()
            if (mime.startsWith("audio/")) {
                extractor.selectTrack(i)
                trackIndex = i
                break
            }
        }
        val audioFormat: MediaFormat = extractor.getTrackFormat(trackIndex)
        val sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE))
            audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        else
            44100

        val channelCount = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
            audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        else
            2
        val mime = audioFormat.getString(MediaFormat.KEY_MIME).toString()

        val decoder = MediaCodec.createDecoderByType(mime)
        val timeoutMicroSeconds = 10000L
        val durationUs = audioFormat.getLong(MediaFormat.KEY_DURATION)
        decoder.configure(audioFormat, null, null, 0)
        decoder.start()
        val bufferInfo = MediaCodec.BufferInfo()

        while(true) {
            val inputBufferIndex = decoder.dequeueInputBuffer(timeoutMicroSeconds)
            if (inputBufferIndex >= 0) {
                val inputBuffer: ByteBuffer = decoder.getInputBuffer(inputBufferIndex)!!
                val sampleSize = extractor.readSampleData(inputBuffer, 0)

                if (durationUs > 12000000L) {
                    return pcmOutput.toByteArray()
                }
                if (sampleSize >= 0) {
                    decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                } else {
                    decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                }
                extractor.advance()


            }

            var outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutMicroSeconds)

            if (outputBufferIndex >= 0) {
                val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                val pcmDataChunk = ByteArray(bufferInfo.size)
                outputBuffer?.position(bufferInfo.offset)
                outputBuffer?.get(pcmDataChunk)
                pcmOutput.write(pcmDataChunk)
                decoder.releaseOutputBuffer(outputBufferIndex, false)

                decoder.releaseOutputBuffer(outputBufferIndex, false)

            }
            if (0 != (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM)) {
                break
            }
        }
        decoder.stop()
        decoder.release()
        extractor.release()

        val outputPcmData = pcmOutput.toByteArray()
        val durationSec = durationUs / 1000000L

        return outputPcmData
    }

}
