package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ArtifactEntity

@Composable
fun DynamicCanvasView(
    artifact: ArtifactEntity?,
    onClearWorkspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSimulatingExecution by remember { mutableStateOf(false) }
    var simulationOutput by remember { mutableStateOf<String?>(null) }

    if (artifact == null) {
        // Empty state
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0C1322))
                .border(1.dp, Color(0x2200F0FF), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = Color(0xFF00ADB5),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "DYNAMIC WORKSPACE READY",
                    color = Color(0xFF00F0FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Instruct JARVIS by voice or prompt to generate code, draft emails, formulate documents, or render structured matrices.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
        return
    }

    val typeIcon = when (artifact.type) {
        "CODE" -> Icons.Default.Code
        "EMAIL" -> Icons.Default.Email
        "TABLE" -> Icons.Default.TableChart
        else -> Icons.Default.Description
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("dynamic_workspace_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1322)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300F0FF))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131D33))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = artifact.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        if (artifact.previewDetails.isNotBlank()) {
                            Text(
                                text = artifact.previewDetails,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Action icons: Copy, Share, Clear
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("JARVIS Deliverable", artifact.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Deliverable copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("copy_deliverable_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Content",
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TITLE, artifact.title)
                                putExtra(Intent.EXTRA_TEXT, artifact.content)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Export JARVIS Deliverable")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("share_deliverable_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export & Share",
                            tint = Color(0xFF64D2FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onClearWorkspace,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("clear_workspace_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear Canvas",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Body rendering according to type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF080D1A))
                    .padding(12.dp)
            ) {
                when (artifact.type) {
                    "CODE" -> {
                        CodeViewer(content = artifact.content)
                    }
                    "TABLE" -> {
                        TableViewer(csvContent = artifact.content)
                    }
                    else -> {
                        DocumentViewer(content = artifact.content)
                    }
                }
            }

            // Code execution simulation footer if type is CODE
            if (artifact.type == "CODE") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A0F1D))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EXECUTION SANDBOX",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedButton(
                            onClick = {
                                isSimulatingExecution = true
                                simulationOutput = "[SANDBOX RUNTIME]\n> Launching ${artifact.title}...\n> Initializing memory space...\n> All 4 unit assertions PASSED (0.012s)\n> Output: Process terminated with status code 0 (SUCCESS)"
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF00F59B)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F59B).copy(alpha = 0.5f)),
                            modifier = Modifier.height(30.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RUN SIMULATION", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (simulationOutput != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF04070F), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0x3300F59B), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = simulationOutput ?: "",
                                color = Color(0xFF00F59B),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CodeViewer(content: String) {
    val scrollStateV = rememberScrollState()
    val scrollStateH = rememberScrollState()
    val lines = content.lines()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollStateV)
            .horizontalScroll(scrollStateH)
    ) {
        // Line numbers column
        Column(
            modifier = Modifier
                .padding(end = 12.dp)
                .background(Color(0x1100F0FF))
                .padding(horizontal = 6.dp)
        ) {
            lines.indices.forEach { index ->
                Text(
                    text = "${index + 1}",
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp
                )
            }
        }

        // Code content
        Column {
            lines.forEach { line ->
                // Simple keyword coloring
                val textColor = when {
                    line.trim().startsWith("#") || line.trim().startsWith("//") || line.trim().startsWith("--") -> Color(0xFF64748B)
                    line.contains("def ") || line.contains("class ") || line.contains("fun ") || line.contains("import ") -> Color(0xFF00F0FF)
                    line.contains("return ") || line.contains("async ") || line.contains("await ") -> Color(0xFF9D4EDD)
                    else -> Color(0xFFE2E8F0)
                }
                Text(
                    text = line.ifEmpty { " " },
                    color = textColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun DocumentViewer(content: String) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(4.dp)
    ) {
        content.lines().forEach { line ->
            when {
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        color = Color(0xFF00F0FF),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        color = Color(0xFF64D2FF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        color = Color(0xFF00ADB5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("• ", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold)
                        Text(
                            text = line.removePrefix("- ").removePrefix("* "),
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
                else -> {
                    Text(
                        text = line,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TableViewer(csvContent: String) {
    val scrollStateV = rememberScrollState()
    val scrollStateH = rememberScrollState()
    val rows = csvContent.lines().filter { it.isNotBlank() }.map { it.split(",") }

    if (rows.isEmpty()) return

    val headers = rows.first()
    val dataRows = rows.drop(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollStateV)
            .horizontalScroll(scrollStateH)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .background(Color(0xFF131D33), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .padding(vertical = 8.dp)
        ) {
            headers.forEach { header ->
                Text(
                    text = header.trim(),
                    color = Color(0xFF00F0FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .width(130.dp)
                        .padding(horizontal = 8.dp)
                )
            }
        }

        // Data Rows
        dataRows.forEachIndexed { index, row ->
            val bg = if (index % 2 == 0) Color(0xFF080D1A) else Color(0xFF0F172A)
            Row(
                modifier = Modifier
                    .background(bg)
                    .border(0.5.dp, Color(0x1100F0FF))
                    .padding(vertical = 8.dp)
            ) {
                headers.indices.forEach { colIndex ->
                    val cell = row.getOrNull(colIndex) ?: ""
                    Text(
                        text = cell.trim(),
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .width(130.dp)
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
