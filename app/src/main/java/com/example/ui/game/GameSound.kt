package com.example.ui.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object GameSound {
    private val scope = CoroutineScope(Dispatchers.Default)
    private const val SAMPLE_RATE = 22050

    private fun playTone(
        frequencyStart: Double,
        frequencyEnd: Double,
        durationMs: Int,
        volume: Float = 0.5f,
        waveType: String = "sine"
    ) {
        scope.launch {
            try {
                val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val progress = i.toDouble() / numSamples
                    val currentFreq = frequencyStart + (frequencyEnd - frequencyStart) * progress
                    
                    val angle = 2.0 * Math.PI * currentFreq * t
                    val rawValue = when (waveType) {
                        "square" -> if (sin(angle) >= 0) 1.0 else -1.0
                        "sawtooth" -> 2.0 * (t * currentFreq - Math.floor(0.5 + t * currentFreq))
                        else -> sin(angle) // sine
                    }

                    // Apply linear fade-out envelope to avoid pops
                    val envelope = if (progress > 0.8) {
                        (1.0 - progress) / 0.2
                    } else if (progress < 0.1) {
                        progress / 0.1
                    } else {
                        1.0
                    }

                    val sampleValue = (rawValue * 32767.0 * volume * envelope).toInt()
                    samples[i] = sampleValue.coerceIn(-32768, 32767).toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                
                // Release audio track resources after playback is complete
                scope.launch {
                    kotlinx.coroutines.delay(durationMs + 100L)
                    try {
                        audioTrack.stop()
                        audioTrack.release()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playSlash() {
        // Laser sweep sound: high to low freq
        playTone(
            frequencyStart = 1500.0,
            frequencyEnd = 300.0,
            durationMs = 120,
            volume = 0.35f,
            waveType = "sawtooth"
        )
    }

    fun playCoin() {
        // Double rising crisp crystal sound (Dopamine plink)
        playTone(
            frequencyStart = 880.0,
            frequencyEnd = 1200.0,
            durationMs = 80,
            volume = 0.4f,
            waveType = "sine"
        )
        scope.launch {
            kotlinx.coroutines.delay(70)
            playTone(
                frequencyStart = 1320.0,
                frequencyEnd = 1760.0,
                durationMs = 120,
                volume = 0.45f,
                waveType = "sine"
            )
        }
    }

    fun playCrit() {
        // Sharp metallic electric ring
        playTone(
            frequencyStart = 2000.0,
            frequencyEnd = 800.0,
            durationMs = 180,
            volume = 0.5f,
            waveType = "square"
        )
    }

    fun playLevelUp() {
        // Triumphant major scale/chord sweep (C4 -> E4 -> G4 -> C5)
        val notes = listOf(261.63, 329.63, 392.00, 523.25)
        scope.launch {
            for (note in notes) {
                playTone(
                    frequencyStart = note,
                    frequencyEnd = note * 1.05,
                    durationMs = 150,
                    volume = 0.5f,
                    waveType = "sine"
                )
                kotlinx.coroutines.delay(100)
            }
            playTone(
                frequencyStart = 523.25,
                frequencyEnd = 1046.50,
                durationMs = 300,
                volume = 0.6f,
                waveType = "sawtooth"
            )
        }
    }

    fun playDamage() {
        // Low explosive crash rumble
        playTone(
            frequencyStart = 120.0,
            frequencyEnd = 40.0,
            durationMs = 250,
            volume = 0.6f,
            waveType = "sawtooth"
        )
    }

    fun playShieldBreak() {
        // Glass shatter pitch plunge
        playTone(
            frequencyStart = 2200.0,
            frequencyEnd = 100.0,
            durationMs = 300,
            volume = 0.5f,
            waveType = "square"
        )
    }

    fun playPowerUp() {
        // Funky sci-fi sweep up
        playTone(
            frequencyStart = 400.0,
            frequencyEnd = 2400.0,
            durationMs = 400,
            volume = 0.45f,
            waveType = "sine"
        )
    }
}
