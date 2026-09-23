package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.ActionLogTerminalView
import com.example.ui.components.DynamicCanvasView
import com.example.ui.components.HudHeader
import com.example.ui.components.JarvisOrbVisualizer
import com.example.ui.components.TaskScheduleView
import com.example.ui.components.VoiceCommandBar

@Composable
fun JarvisMainScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val schedule by viewModel.schedule.collectAsState()
    val logs by viewModel.actionLogs.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.initVoice(context)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            viewModel.initVoice(context)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("jarvis_main_scaffold"),
        containerColor = Color(0xFF050811),
        topBar = {
            HudHeader(
                isAlwaysListening = uiState.isAlwaysListening,
                isMuted = uiState.isMuted,
                selectedTab = uiState.selectedWorkspaceTab,
                onTabSelected = { viewModel.setSelectedTab(it) },
                onToggleAlwaysListening = { viewModel.toggleAlwaysListening() },
                onToggleMute = { viewModel.toggleMute() }
            )
        },
        bottomBar = {
            VoiceCommandBar(
                listeningState = uiState.listeningState,
                lastSpeechSnippet = uiState.lastSpeechSnippet,
                onSendCommand = { command ->
                    viewModel.executeVoiceCommand(command)
                },
                onToggleMic = {
                    if (!hasAudioPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.toggleManualListen()
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF050811)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Glowing Arc Reactor Orb
            JarvisOrbVisualizer(
                listeningState = uiState.listeningState,
                isSpeaking = uiState.isSpeaking,
                isProcessing = uiState.isProcessing,
                audioRmsDb = uiState.audioRmsDb,
                wakePulseTriggered = uiState.wakePulseTriggered,
                onOrbClick = {
                    if (!hasAudioPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.toggleManualListen()
                    }
                },
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )

            // Dynamic Status HUD Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.activeStatusMessage,
                    color = Color(0xFF64D2FF),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Dynamic Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (uiState.selectedWorkspaceTab) {
                    0 -> {
                        // Dynamic Output Workspace / Canvas
                        DynamicCanvasView(
                            artifact = uiState.activeArtifact,
                            onClearWorkspace = { viewModel.clearWorkspace() }
                        )
                    }
                    1 -> {
                        // Task & Schedule Matrix
                        TaskScheduleView(
                            tasks = tasks,
                            schedule = schedule,
                            onToggleTask = { viewModel.toggleTask(it) },
                            onDeleteTask = { viewModel.deleteTask(it) },
                            onAddTask = { title, priority -> viewModel.addTask(title, priority) },
                            onToggleSchedule = { viewModel.toggleSchedule(it) },
                            onAddSchedule = { title, time -> viewModel.addSchedule(title, time) }
                        )
                    }
                    2 -> {
                        // Action Log Terminal
                        ActionLogTerminalView(
                            logs = logs,
                            onClearLogs = { viewModel.clearLogs() }
                        )
                    }
                }
            }
        }
    }
}
