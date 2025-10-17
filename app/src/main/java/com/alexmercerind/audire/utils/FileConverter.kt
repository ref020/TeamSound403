package com.alexmercerind.audire.utils
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scaleMatrix

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.builtins.MapEntrySerializer

data class PcmOutputData(
    val pcmOutputData: ByteArray,
    val sampleRate: Int,
    val channels: Int,
    val durationSec: Int
)
class FileConverter() {
    var returnData = null

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

    fun readFile(context: Context?, inputFileUri: Uri): PcmOutputData {
        val pcmDataBufferList = mutableListOf<ByteArray>()
        val pfd = context!!.contentResolver.openFileDescriptor(inputFileUri, "r")
            ?: throw IllegalArgumentException("Cannot open URI: $inputFileUri")
        println(pfd.fileDescriptor)
        extractor.setDataSource(pfd.fileDescriptor)
        println("extractor file path set")
        val numTracks = extractor.trackCount
            for (i in 0 until numTracks) {
            val audioFormat: MediaFormat = extractor.getTrackFormat(i)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME).toString()
            if (mime.startsWith("audio/")) {
                extractor.selectTrack(i)
                trackIndex = i
                break
            }
        }
        println("track selected")
        val inputAudioFormat: MediaFormat = extractor.getTrackFormat(trackIndex)
//        val sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE))
//            audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
//        else
//            44100
//
//        val channelCount = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
//            audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
//        else
//            2
        val mime = inputAudioFormat.getString(MediaFormat.KEY_MIME).toString()

        val decoder = MediaCodec.createDecoderByType(mime)
        //val timeoutMicroSeconds = 10000L
        //val durationUs = audioFormat.getLong(MediaFormat.KEY_DURATION)
        decoder.configure(inputAudioFormat, null, null, 0)
        decoder.start()
        val bufferInfo = MediaCodec.BufferInfo()
        var totalOutputBytes = 0L
        var endOfStream = false

        while(!endOfStream) {
            val inputBufferIndex = decoder.dequeueInputBuffer(10_000)
            if (inputBufferIndex >= 0) {
                val inputBuffer: ByteBuffer = decoder.getInputBuffer(inputBufferIndex)!!
                val sampleSize = extractor.readSampleData(inputBuffer, 0)

                //if (durationUs > 12000000L) {
                //    return pcmOutput.toByteArray()
                //}
                if (sampleSize >= 0) {
                    decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                    extractor.advance()
                } else {
                    decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    println("final chunk queued to decoder")
                    endOfStream = true
                }
            }

            var outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10_000)

            if (outputBufferIndex >= 0) {
                val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)!!
                val pcmDataChunk = ByteArray(bufferInfo.size)
                outputBuffer.get(pcmDataChunk)
                outputBuffer.clear()
                pcmDataBufferList.add(pcmDataChunk)
                totalOutputBytes += bufferInfo.size
                println(pcmDataChunk)
                decoder.releaseOutputBuffer(outputBufferIndex, false)

            }
//            if (0 != (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM)) {
//                break
//            }
        }
        decoder.stop()
        decoder.release()
        extractor.release()
        pfd.close()

        val outputPcmData = pcmOutput.toByteArray()
        //val durationSec = durationUs / 1000000L
        val pcmOutput = ByteArray(totalOutputBytes.toInt())
        var offset = 0
        for (buffer in pcmDataBufferList) {
            System.arraycopy(buffer, 0, pcmOutput, offset, buffer.size)
            offset += buffer.size
        }

        val sampleRate = inputAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = if (inputAudioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
            inputAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1
        val durationUs = inputAudioFormat.getLong(MediaFormat.KEY_DURATION)

        return PcmOutputData(
            pcmOutputData = pcmOutput,
            sampleRate = sampleRate,
            channels = channels,
            durationSec = (durationUs / 1000000L).toInt()
        )
    }

}

annotation class PcmData
