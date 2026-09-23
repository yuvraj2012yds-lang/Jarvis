package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class JarvisListeningState {
    IDLE,
    WAITING_FOR_WAKE_WORD,
    WAKE_WORD_TRIGGERED,
    RECORDING_COMMAND,
    PROCESSING
}

class JarvisVoiceManager(
    private val context: Context,
    private val onWakeWordDetected: (command: String?) -> Unit,
    private val onCommandReceived: (command: String) -> Unit,
    private val onError: (message: String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _listeningState = MutableStateFlow(JarvisListeningState.IDLE)
    val listeningState: StateFlow<JarvisListeningState> = _listeningState.asStateFlow()

    private val _isAlwaysListening = MutableStateFlow(true)
    val isAlwaysListening: StateFlow<Boolean> = _isAlwaysListening.asStateFlow()

    private val _audioRmsDb = MutableStateFlow(0f)
    val audioRmsDb: StateFlow<Float> = _audioRmsDb.asStateFlow()

    private val _lastPartialSpeech = MutableStateFlow("")
    val lastPartialSpeech: StateFlow<String> = _lastPartialSpeech.asStateFlow()

    private var isManuallyStopped = false
    private var awaitingDirectCommandAfterWake = false

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
        } catch (e: Exception) {
            Log.e("JarvisVoice", "ToneGenerator init error: ${e.message}")
        }
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("JarvisVoice", "Speech recognition not available on device")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (_listeningState.value != JarvisListeningState.WAKE_WORD_TRIGGERED) {
                            _listeningState.value = if (awaitingDirectCommandAfterWake) {
                                JarvisListeningState.RECORDING_COMMAND
                            } else {
                                JarvisListeningState.WAITING_FOR_WAKE_WORD
                            }
                        }
                    }

                    override fun onBeginningOfSpeech() {
                        _audioRmsDb.value = 5f
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize roughly 0..10
                        val normalized = ((rmsdB + 2f).coerceAtLeast(0f) / 12f) * 10f
                        _audioRmsDb.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _audioRmsDb.value = 0f
                    }

                    override fun onError(error: Int) {
                        _audioRmsDb.value = 0f
                        val errorMessage = getErrorText(error)
                        Log.d("JarvisVoice", "SpeechRecognizer error: $errorMessage (code $error)")

                        // If always-listening is enabled and not manually stopped, restart gracefully
                        if (!isManuallyStopped && _isAlwaysListening.value) {
                            mainHandler.postDelayed({
                                if (!isManuallyStopped) {
                                    startListeningInternal()
                                }
                            }, 500)
                        } else {
                            _listeningState.value = JarvisListeningState.IDLE
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _audioRmsDb.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()?.trim() ?: ""
                        _lastPartialSpeech.value = spokenText

                        handleSpokenText(spokenText)

                        // If always listening and we're not currently processing a major action, cycle back to listen
                        if (!isManuallyStopped && _isAlwaysListening.value && !awaitingDirectCommandAfterWake) {
                            mainHandler.postDelayed({
                                if (!isManuallyStopped && _isAlwaysListening.value) {
                                    startListeningInternal()
                                }
                            }, 600)
                        } else if (!awaitingDirectCommandAfterWake) {
                            _listeningState.value = JarvisListeningState.IDLE
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull() ?: ""
                        if (partial.isNotBlank()) {
                            _lastPartialSpeech.value = partial
                            // Check if wake word "jarvis" is present mid-speech
                            if (!awaitingDirectCommandAfterWake && containsWakeWord(partial)) {
                                triggerHapticAndChime()
                                _listeningState.value = JarvisListeningState.WAKE_WORD_TRIGGERED
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } catch (e: Exception) {
            Log.e("JarvisVoice", "Failed to init SpeechRecognizer: ${e.message}")
        }
    }

    private fun handleSpokenText(spokenText: String) {
        if (spokenText.isBlank()) return

        val lower = spokenText.lowercase(Locale.ROOT)

        if (awaitingDirectCommandAfterWake) {
            // Already woke, treat entire phrase as command
            awaitingDirectCommandAfterWake = false
            _listeningState.value = JarvisListeningState.PROCESSING
            onCommandReceived(spokenText)
            return
        }

        if (containsWakeWord(lower)) {
            triggerHapticAndChime()
            _listeningState.value = JarvisListeningState.WAKE_WORD_TRIGGERED

            // Extract command after "jarvis"
            val command = extractCommandAfterWakeWord(spokenText)
            if (command.isNotBlank()) {
                _listeningState.value = JarvisListeningState.PROCESSING
                onWakeWordDetected(command)
                onCommandReceived(command)
            } else {
                // User just said "Jarvis", notify and wait for command
                awaitingDirectCommandAfterWake = true
                onWakeWordDetected(null)
                mainHandler.postDelayed({
                    startListeningInternal()
                }, 400)
            }
        } else {
            // Wake word wasn't explicitly said; if always-listening is off, or user pressed mic button
            if (!_isAlwaysListening.value) {
                _listeningState.value = JarvisListeningState.PROCESSING
                onCommandReceived(spokenText)
            }
        }
    }

    private fun containsWakeWord(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return lower.contains("jarvis")
    }

    private fun extractCommandAfterWakeWord(text: String): String {
        val regex = Regex("(?i)\\bjarvis\\b[,.:;!\\s]*", RegexOption.IGNORE_CASE)
        val parts = text.split(regex, limit = 2)
        return if (parts.size > 1) parts[1].trim() else ""
    }

    fun triggerHapticAndChime() {
        try {
            // Haptic vibration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
            // Sound chime
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            Log.w("JarvisVoice", "Haptic/chime error: ${e.message}")
        }
    }

    fun startListening() {
        isManuallyStopped = false
        startListeningInternal()
    }

    private fun startListeningInternal() {
        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        try {
            speechRecognizer?.startListening(intent)
            _listeningState.value = if (awaitingDirectCommandAfterWake) {
                JarvisListeningState.RECORDING_COMMAND
            } else {
                JarvisListeningState.WAITING_FOR_WAKE_WORD
            }
        } catch (e: Exception) {
            Log.e("JarvisVoice", "Error starting listening: ${e.message}")
            _listeningState.value = JarvisListeningState.IDLE
        }
    }

    fun stopListening() {
        isManuallyStopped = true
        awaitingDirectCommandAfterWake = false
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w("JarvisVoice", "Error stopping listening: ${e.message}")
        }
        _listeningState.value = JarvisListeningState.IDLE
        _audioRmsDb.value = 0f
    }

    fun toggleAlwaysListening() {
        val newVal = !_isAlwaysListening.value
        _isAlwaysListening.value = newVal
        if (newVal) {
            startListening()
        } else {
            stopListening()
        }
    }

    fun setAlwaysListening(enabled: Boolean) {
        _isAlwaysListening.value = enabled
        if (enabled) {
            startListening()
        } else {
            stopListening()
        }
    }

    fun destroy() {
        isManuallyStopped = true
        try {
            speechRecognizer?.destroy()
            toneGenerator?.release()
        } catch (e: Exception) {
            Log.w("JarvisVoice", "Error destroying speech components: ${e.message}")
        }
        speechRecognizer = null
        toneGenerator = null
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Speech recognition error ($errorCode)"
        }
    }
}
