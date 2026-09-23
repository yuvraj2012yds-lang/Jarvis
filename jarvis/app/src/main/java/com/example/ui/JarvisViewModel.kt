package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agent.AgentExecutionResult
import com.example.agent.JarvisAgentEngine
import com.example.data.ActionLogEntity
import com.example.data.ArtifactEntity
import com.example.data.JarvisDatabase
import com.example.data.ScheduleEntity
import com.example.data.TaskEntity
import com.example.voice.JarvisListeningState
import com.example.voice.JarvisTtsManager
import com.example.voice.JarvisVoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class JarvisUiState(
    val listeningState: JarvisListeningState = JarvisListeningState.IDLE,
    val isAlwaysListening: Boolean = true,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val audioRmsDb: Float = 0f,
    val lastSpeechSnippet: String = "",
    val activeStatusMessage: String = "SYSTEM ONLINE // LISTENING FOR \"JARVIS\"",
    val selectedWorkspaceTab: Int = 0, // 0 = Canvas, 1 = Tasks & Schedule, 2 = Action Logs
    val isProcessing: Boolean = false,
    val activeArtifact: ArtifactEntity? = null,
    val wakePulseTriggered: Boolean = false
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val database = JarvisDatabase.getDatabase(application, viewModelScope)
    private val dao = database.jarvisDao()
    private val agentEngine = JarvisAgentEngine(dao)

    private val ttsManager = JarvisTtsManager(application)
    private var voiceManager: JarvisVoiceManager? = null

    private val _uiState = MutableStateFlow(JarvisUiState())
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    val tasks: StateFlow<List<TaskEntity>> = dao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schedule: StateFlow<List<ScheduleEntity>> = dao.getAllSchedule()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actionLogs: StateFlow<List<ActionLogEntity>> = dao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestArtifact: StateFlow<ArtifactEntity?> = dao.getLatestArtifact()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Collect latest artifact into UI state
        viewModelScope.launch {
            latestArtifact.collect { artifact ->
                if (artifact != null) {
                    _uiState.value = _uiState.value.copy(activeArtifact = artifact)
                }
            }
        }

        // Collect TTS speaking state
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { isSpeaking ->
                _uiState.value = _uiState.value.copy(isSpeaking = isSpeaking)
            }
        }

        // Collect TTS muted state
        viewModelScope.launch {
            ttsManager.isMuted.collect { isMuted ->
                _uiState.value = _uiState.value.copy(isMuted = isMuted)
            }
        }
    }

    fun initVoice(context: Context) {
        if (voiceManager != null) return

        voiceManager = JarvisVoiceManager(
            context = context,
            onWakeWordDetected = { cmd ->
                onWakeTriggered(cmd)
            },
            onCommandReceived = { cmd ->
                executeVoiceCommand(cmd)
            },
            onError = { errMsg ->
                _uiState.value = _uiState.value.copy(
                    activeStatusMessage = "AUDIO SENSOR NOTICE: $errMsg"
                )
            }
        )

        // Observe voice manager flows
        viewModelScope.launch {
            voiceManager?.listeningState?.collect { state ->
                _uiState.value = _uiState.value.copy(listeningState = state)
            }
        }

        viewModelScope.launch {
            voiceManager?.isAlwaysListening?.collect { always ->
                _uiState.value = _uiState.value.copy(isAlwaysListening = always)
            }
        }

        viewModelScope.launch {
            voiceManager?.audioRmsDb?.collect { rms ->
                _uiState.value = _uiState.value.copy(audioRmsDb = rms)
            }
        }

        viewModelScope.launch {
            voiceManager?.lastPartialSpeech?.collect { partial ->
                if (partial.isNotBlank()) {
                    _uiState.value = _uiState.value.copy(lastSpeechSnippet = partial)
                }
            }
        }

        // Start listening automatically
        voiceManager?.startListening()
    }

    private fun onWakeTriggered(commandAfterWake: String?) {
        _uiState.value = _uiState.value.copy(
            wakePulseTriggered = true,
            activeStatusMessage = "WAKE WORD DETECTED // AWAITING DIRECTIVE"
        )
        if (commandAfterWake.isNullOrBlank()) {
            ttsManager.speak("At your service, sir.")
        }
        // Auto reset pulse state after 1.5 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            _uiState.value = _uiState.value.copy(wakePulseTriggered = false)
        }
    }

    fun executeVoiceCommand(command: String) {
        if (command.isBlank()) return

        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            lastSpeechSnippet = command,
            activeStatusMessage = "EXECUTING DIRECTIVE: \"$command\""
        )

        viewModelScope.launch {
            val result = agentEngine.executeCommand(command)
            _uiState.value = _uiState.value.copy(isProcessing = false)

            when (result) {
                is AgentExecutionResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        activeStatusMessage = "DIRECTIVE EXECUTED // ALL SYSTEMS NOMINAL",
                        activeArtifact = result.artifact ?: _uiState.value.activeArtifact
                    )

                    // Automatically switch to the relevant tab
                    when (result.actionType) {
                        "TASK_CREATED", "SCHEDULE_UPDATED" -> {
                            _uiState.value = _uiState.value.copy(selectedWorkspaceTab = 1)
                        }
                        "DELIVERABLE_GENERATED", "SYSTEM_DIAGNOSTICS" -> {
                            _uiState.value = _uiState.value.copy(selectedWorkspaceTab = 0)
                        }
                    }

                    // Speak punchy concise response
                    ttsManager.speak(result.spokenResponse)
                }

                is AgentExecutionResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        activeStatusMessage = "DIRECTIVE ANOMALY: ${result.errorMessage}"
                    )
                    ttsManager.speak(result.spokenResponse)
                }
            }
        }
    }

    fun toggleAlwaysListening() {
        voiceManager?.toggleAlwaysListening()
    }

    fun toggleManualListen() {
        val current = _uiState.value.listeningState
        if (current == JarvisListeningState.IDLE) {
            voiceManager?.triggerHapticAndChime()
            voiceManager?.startListening()
        } else {
            voiceManager?.stopListening()
        }
    }

    fun toggleMute() {
        ttsManager.toggleMute()
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedWorkspaceTab = tabIndex)
    }

    // Task Actions
    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            dao.setTaskCompleted(task.id, !task.isCompleted)
            ttsManager.speak(if (!task.isCompleted) "Task completed, sir." else "Task marked active.")
        }
    }

    fun addTask(title: String, priority: String) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title.trim(),
                priority = priority,
                category = "Direct Entry"
            )
            dao.insertTask(task)
            dao.insertLog(
                ActionLogEntity(
                    command = "MANUAL_TASK_ADD",
                    actionType = "TASK_CREATED",
                    summary = "Added task: $title ($priority)",
                    status = "SUCCESS"
                )
            )
            ttsManager.speak("Task added, sir.")
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            dao.deleteTask(task)
        }
    }

    // Schedule Actions
    fun toggleSchedule(schedule: ScheduleEntity) {
        viewModelScope.launch {
            dao.setScheduleCompleted(schedule.id, !schedule.isCompleted)
        }
    }

    fun addSchedule(title: String, time: String) {
        viewModelScope.launch {
            val item = ScheduleEntity(
                time = time.trim(),
                title = title.trim(),
                description = "Manual schedule addition"
            )
            dao.insertSchedule(item)
            dao.insertLog(
                ActionLogEntity(
                    command = "MANUAL_SCHEDULE_ADD",
                    actionType = "SCHEDULE_UPDATED",
                    summary = "Added schedule item: $title at $time",
                    status = "SUCCESS"
                )
            )
            ttsManager.speak("Schedule updated, sir.")
        }
    }

    fun clearWorkspace() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(activeArtifact = null)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            dao.clearLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager?.destroy()
        ttsManager.destroy()
    }
}
