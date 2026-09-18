package com.balsis.app.ui.sheet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.balsis.app.ai.GeminiTranscriber
import com.balsis.app.data.db.AppDatabase
import com.balsis.app.data.model.TranscriptionEntity
import com.balsis.app.data.repository.ApiKeyRepository
import com.balsis.app.service.RecentVoiceNoteCache
import com.balsis.app.ui.theme.BalsisTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class TranscribeSheetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val audioUris = extractAudioUris()
        if (audioUris.isEmpty()) {
            Toast.makeText(this, "Nav atrasts audio fails.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            BalsisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        TranscribeSheetContent(
                            audioUris = audioUris,
                            onDismiss = { finish() }
                        )
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun extractAudioUris(): List<Uri> {
        val list = mutableListOf<Uri>()
        val action = intent.action
        if (action == android.content.Intent.ACTION_SEND) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableExtra<Uri>(android.content.Intent.EXTRA_STREAM)
            }
            if (uri != null) list.add(uri)
        } else if (action == android.content.Intent.ACTION_SEND_MULTIPLE) {
            val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableArrayListExtra<Uri>(android.content.Intent.EXTRA_STREAM)
            }
            if (uris != null) list.addAll(uris)
        }
        return list
    }
}

sealed interface SheetState {
    data object Loading : SheetState
    data class Success(val results: List<TranscribeItemUi>) : SheetState
    data class Error(val message: String) : SheetState
}

data class TranscribeItemUi(
    var senderName: String,
    var chatName: String,
    val summary: String,
    val fullText: String,
    val contextInfo: String,
    var isSaved: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscribeSheetContent(
    audioUris: List<Uri>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val apiKeyRepo = remember { ApiKeyRepository(context) }
    val db = remember { AppDatabase.getInstance(context) }

    var sheetState by remember { mutableStateOf<SheetState>(SheetState.Loading) }

    LaunchedEffect(Unit) {
        val apiKey = apiKeyRepo.getApiKey()
        if (apiKey.isBlank()) {
            sheetState = SheetState.Error("Lūdzu, ievadiet savu Google AI Studio API atslēgu Balsis lietotnē.")
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val matchedNotif = RecentVoiceNoteCache.findBestMatch()
                val detectedSender = matchedNotif?.senderName ?: "Balss ziņa"
                val detectedChat = matchedNotif?.chatName ?: "WhatsApp"

                val transcriber = GeminiTranscriber(apiKey)
                val items = mutableListOf<TranscribeItemUi>()

                for (uri in audioUris) {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.use { it.readBytes() } ?: ByteArray(0)

                    if (bytes.isNotEmpty()) {
                        val result = transcriber.transcribeAudio(bytes)
                        if (result.isSuccess) {
                            val data = result.getOrThrow()
                            val item = TranscribeItemUi(
                                senderName = detectedSender,
                                chatName = detectedChat,
                                summary = data.summary,
                                fullText = data.fullText,
                                contextInfo = data.detectedContext
                            )
                            items.add(item)

                            // Save into local Room database for searchable history
                            db.transcriptionDao().insert(
                                TranscriptionEntity(
                                    senderName = item.senderName,
                                    chatName = item.chatName,
                                    summary = item.summary,
                                    fullText = item.fullText
                                )
                            )
                        } else {
                            throw result.exceptionOrNull() ?: Exception("Kļūda transkribējot audio.")
                        }
                    }
                }

                sheetState = SheetState.Success(items)
            } catch (e: Exception) {
                sheetState = SheetState.Error(e.localizedMessage ?: "Nezināma kļūda apstrādājot audio.")
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Balsis • Kopsavilkums",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Aizvērt")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (val state = sheetState) {
                is SheetState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Klausās un veido kopsavilkumu...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is SheetState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = "Kļūda:",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Labi")
                        }
                    }
                }

                is SheetState.Success -> {
                    state.results.forEachIndexed { index, item ->
                        TranscribeResultCard(item = item)
                        if (index < state.results.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledTonalButton(onClick = onDismiss) {
                            Text("Aizvērt")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TranscribeResultCard(item: TranscribeItemUi) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Sender / Chat Tag
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${item.senderName} (${item.chatName})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // High contrast summary box
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "BŪTĪBA (TL;DR):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Full transcription expander
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isExpanded) "Slēpt pilno tekstu" else "Skatīt pilno transkripciju",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = item.fullText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Copy button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Balsis Transkripcija", "${item.summary}\n\n${item.fullText}")
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Nokopēts starpliktuvē!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kopēt tekstu")
            }
        }
    }
}
