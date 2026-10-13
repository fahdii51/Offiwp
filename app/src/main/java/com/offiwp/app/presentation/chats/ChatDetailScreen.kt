package com.offiwp.app.presentation.chats

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.offiwp.app.domain.model.Message
import com.offiwp.app.domain.model.MessageStatus
import com.offiwp.app.domain.model.MessageType
import com.offiwp.app.presentation.MainViewModel
import com.offiwp.app.ui.theme.CheckReadBlue
import com.offiwp.app.ui.theme.IncomingBubbleDark
import com.offiwp.app.ui.theme.OutgoingBubbleDark
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages by viewModel.chatRepository.getMessagesForChat(chatId).collectAsState(initial = emptyList())

    var inputText by remember { mutableStateOf("") }
    var replyToMessage by remember { mutableStateOf<Message?>(null) }
    var selectedMessageForReaction by remember { mutableStateOf<Message?>(null) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    // Voice recording state
    var isRecordingVoiceNote by remember { mutableStateOf(false) }
    val recordAmplitude by viewModel.audioRecorder.amplitude.collectAsState()
    val recordDurationMs by viewModel.audioRecorder.recordingDurationMs.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
        viewModel.chatRepository.markChatAsRead(chatId)
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val file = File(context.cacheDir, "sent_img_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(it)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                viewModel.chatRepository.sendMessage(
                    chatId = chatId,
                    content = "[Image]",
                    type = MessageType.IMAGE,
                    mediaFile = file
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (chatId.startsWith("group_")) "Offi Group Chat" else "Offline Contact ($chatId)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(com.offiwp.app.ui.theme.StatusOnlineGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Bluetooth P2P Online",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("chat_detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                items(messages, key = { it.messageId }) { msg ->
                    MessageBubbleRow(
                        message = msg,
                        viewModel = viewModel,
                        onLongClick = { selectedMessageForReaction = msg },
                        onReplyClick = { replyToMessage = msg }
                    )
                }
            }

            // Reaction Dialog Overlay
            selectedMessageForReaction?.let { msg ->
                EmojiReactionSelector(
                    onEmojiSelected = { emoji ->
                        scope.launch {
                            viewModel.chatRepository.addReaction(chatId, msg.messageId, emoji)
                            selectedMessageForReaction = null
                        }
                    },
                    onDismiss = { selectedMessageForReaction = null }
                )
            }

            // Quoted Reply Preview Bar
            replyToMessage?.let { replyMsg ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Replying to ${replyMsg.senderName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = replyMsg.content,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = { replyToMessage = null }) {
                        Text("✕", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // Attachment options popup
            AnimatedVisibility(visible = showAttachmentMenu) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    IconButton(onClick = {
                        showAttachmentMenu = false
                        photoPickerLauncher.launch("image/*")
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Image, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.primary)
                            Text("Gallery", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(onClick = {
                        showAttachmentMenu = false
                        photoPickerLauncher.launch("image/*")
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", tint = MaterialTheme.colorScheme.primary)
                            Text("Camera", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Bottom Input Section: Text or Live Voice Note Recording Bar
            if (isRecordingVoiceNote) {
                // Waveform & Recording Controller
                VoiceRecordingBar(
                    amplitude = recordAmplitude,
                    durationMs = recordDurationMs,
                    onCancel = {
                        viewModel.audioRecorder.cancelRecording()
                        isRecordingVoiceNote = false
                    },
                    onSend = {
                        val (file, duration) = viewModel.audioRecorder.stopRecording()
                        isRecordingVoiceNote = false
                        if (file != null && duration > 500) {
                            scope.launch {
                                viewModel.chatRepository.sendMessage(
                                    chatId = chatId,
                                    content = "[Voice Note]",
                                    type = MessageType.VOICE,
                                    mediaFile = file,
                                    durationMs = duration,
                                    replyToId = replyToMessage?.messageId,
                                    replyToText = replyToMessage?.content
                                )
                                replyToMessage = null
                            }
                        }
                    }
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    IconButton(onClick = { showAttachmentMenu = !showAttachmentMenu }) {
                        Icon(
                            Icons.Filled.AttachFile,
                            contentDescription = "Attach File",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type an offline message...", style = MaterialTheme.typography.bodyMedium) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_input_field")
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    if (inputText.isBlank()) {
                        // Mic Button for Voice Note
                        IconButton(
                            onClick = {
                                val file = viewModel.audioRecorder.startRecording()
                                if (file != null) {
                                    isRecordingVoiceNote = true
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("record_mic_button")
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = "Record Voice Note", tint = Color.White)
                        }
                    } else {
                        // Send Text Button
                        IconButton(
                            onClick = {
                                val text = inputText
                                inputText = ""
                                val currentReply = replyToMessage
                                replyToMessage = null
                                scope.launch {
                                    viewModel.chatRepository.sendMessage(
                                        chatId = chatId,
                                        content = text,
                                        type = MessageType.TEXT,
                                        replyToId = currentReply?.messageId,
                                        replyToText = currentReply?.content
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("send_message_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleRow(
    message: Message,
    viewModel: MainViewModel,
    onLongClick: () -> Unit,
    onReplyClick: () -> Unit
) {
    val isOutgoing = message.isOutgoing
    val bubbleColor = if (isOutgoing) OutgoingBubbleDark else IncomingBubbleDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOutgoing) 16.dp else 2.dp,
                bottomEnd = if (isOutgoing) 2.dp else 16.dp
            ),
            modifier = Modifier
                .width(280.dp)
                .combinedClickable(
                    onClick = onReplyClick,
                    onLongClick = onLongClick
                )
                .testTag("message_bubble_${message.messageId}")
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (!isOutgoing) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Quoted reply snippet
                message.replyToText?.let { reply ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = reply,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Content based on MessageType
                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                    MessageType.IMAGE -> {
                        message.mediaUri?.let { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Shared Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                    MessageType.VOICE -> {
                        VoiceNotePlayerCard(
                            audioUri = message.mediaUri,
                            durationMs = message.durationMs,
                            viewModel = viewModel
                        )
                    }
                    MessageType.FILE -> {
                        Text(
                            text = "📄 ${message.content}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Footer: Reactions + Time + Delivery Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (message.reactions.isNotEmpty()) {
                        Text(
                            text = message.reactions.values.joinToString(" "),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            MessageStatus.PENDING -> Icon(
                                Icons.Filled.Schedule,
                                contentDescription = "Pending",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            MessageStatus.SENT -> Icon(
                                Icons.Filled.Check,
                                contentDescription = "Sent",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            MessageStatus.DELIVERED -> Icon(
                                Icons.Filled.DoneAll,
                                contentDescription = "Delivered",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            MessageStatus.READ -> Icon(
                                Icons.Filled.DoneAll,
                                contentDescription = "Read",
                                tint = CheckReadBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceNotePlayerCard(
    audioUri: String?,
    durationMs: Long,
    viewModel: MainViewModel
) {
    val isPlaying by viewModel.audioPlayer.isPlaying.collectAsState()
    val currentPosition by viewModel.audioPlayer.currentPosition.collectAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        IconButton(
            onClick = {
                audioUri?.let { viewModel.audioPlayer.playAudio(it) }
            },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Play/Pause Voice Note",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            val total = if (durationMs > 0) durationMs else 1000L
            val progress = (currentPosition.toFloat() / total.toFloat()).coerceIn(0f, 1f)

            Slider(
                value = progress,
                onValueChange = { newProgress ->
                    viewModel.audioPlayer.seekTo((newProgress * total).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = formatDuration(if (isPlaying) currentPosition else durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun VoiceRecordingBar(
    amplitude: Float,
    durationMs: Long,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color.Red.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatDuration(durationMs),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Waveform Visualizer simulation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            repeat(12) { index ->
                val h = (amplitude * 24f + ((index % 4) * 3f)).coerceIn(4f, 28f)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .width(3.dp)
                        .height(h.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        IconButton(onClick = onCancel) {
            Icon(Icons.Filled.MicOff, contentDescription = "Cancel Recording", tint = MaterialTheme.colorScheme.error)
        }

        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Voice Note", tint = Color.White)
        }
    }
}

@Composable
fun EmojiReactionSelector(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val emojis = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp,
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            for (emoji in emojis) {
                Text(
                    text = emoji,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onEmojiSelected(emoji) }
                        .padding(8.dp)
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
