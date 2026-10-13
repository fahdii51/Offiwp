package com.offiwp.app.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.offiwp.app.data.bluetooth.BluetoothManager
import com.offiwp.app.data.bluetooth.BluetoothPacket
import com.offiwp.app.data.local.AppDatabase
import com.offiwp.app.data.local.entities.ChatEntity
import com.offiwp.app.data.local.entities.ContactEntity
import com.offiwp.app.data.local.entities.MessageEntity
import com.offiwp.app.data.local.entities.PendingMessageEntity
import com.offiwp.app.domain.model.Chat
import com.offiwp.app.domain.model.ChatType
import com.offiwp.app.domain.model.Message
import com.offiwp.app.domain.model.MessageStatus
import com.offiwp.app.domain.model.MessageType
import com.offiwp.app.domain.model.UserProfile
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ChatRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val bluetoothManager: BluetoothManager,
    private val profileRepository: ProfileRepository
) {

    private val tag = "ChatRepository"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val mapAdapter = moshi.adapter<Map<String, String>>(
        Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    )
    private val listAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    val allChats: Flow<List<Chat>> = database.chatDao().getAllChats().map { entities ->
        entities.map { entity ->
            val participantList = try {
                listAdapter.fromJson(entity.participantAddressesJson) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            Chat(
                chatId = entity.chatId,
                title = entity.title,
                avatarUri = entity.avatarUri,
                type = if (entity.type == "GROUP") ChatType.GROUP else ChatType.SINGLE,
                participantAddresses = participantList,
                lastMessageText = entity.lastMessageText,
                lastMessageTime = entity.lastMessageTime,
                unreadCount = entity.unreadCount,
                isOnline = entity.isOnline
            )
        }
    }

    init {
        // Listen for incoming Bluetooth packets
        scope.launch {
            bluetoothManager.incomingPackets.collect { packet ->
                handleIncomingPacket(packet)
            }
        }

        // Listen for socket connection status changes to flush pending messages
        scope.launch {
            bluetoothManager.connectedAddresses.collect { connectedSet ->
                for (address in connectedSet) {
                    flushPendingQueueForDevice(address)
                    database.chatDao().updateChatOnlineStatus(address, true)
                }
            }
        }
    }

    fun getMessagesForChat(chatId: String): Flow<List<Message>> {
        return database.messageDao().getMessagesForChat(chatId).map { entities ->
            entities.map { entity ->
                val reactionsMap = try {
                    mapAdapter.fromJson(entity.reactionsJson) ?: emptyMap()
                } catch (_: Exception) {
                    emptyMap()
                }
                Message(
                    messageId = entity.messageId,
                    chatId = entity.chatId,
                    senderAddress = entity.senderAddress,
                    senderName = entity.senderName,
                    content = entity.content,
                    type = try { MessageType.valueOf(entity.type) } catch (_: Exception) { MessageType.TEXT },
                    mediaUri = entity.mediaUri,
                    durationMs = entity.durationMs,
                    timestamp = entity.timestamp,
                    status = try { MessageStatus.valueOf(entity.status) } catch (_: Exception) { MessageStatus.SENT },
                    replyToId = entity.replyToId,
                    replyToText = entity.replyToText,
                    reactions = reactionsMap,
                    isOutgoing = entity.isOutgoing
                )
            }
        }
    }

    suspend fun createOrGetSingleChat(targetAddress: String, name: String): String {
        val existing = database.chatDao().getChatById(targetAddress)
        if (existing != null) return existing.chatId

        val participantsJson = listAdapter.toJson(listOf(targetAddress))
        val newChat = ChatEntity(
            chatId = targetAddress,
            title = name,
            avatarUri = null,
            type = "SINGLE",
            participantAddressesJson = participantsJson,
            lastMessageText = "Start messaging offline",
            lastMessageTime = System.currentTimeMillis(),
            unreadCount = 0,
            isOnline = bluetoothManager.connectedAddresses.value.contains(targetAddress)
        )
        database.chatDao().insertOrUpdateChat(newChat)
        return targetAddress
    }

    suspend fun createGroupChat(groupTitle: String, memberAddresses: List<String>): String {
        val groupId = "group_" + UUID.randomUUID().toString().take(8)
        val participantsJson = listAdapter.toJson(memberAddresses)
        val newGroup = ChatEntity(
            chatId = groupId,
            title = groupTitle,
            avatarUri = null,
            type = "GROUP",
            participantAddressesJson = participantsJson,
            lastMessageText = "Group created",
            lastMessageTime = System.currentTimeMillis(),
            unreadCount = 0,
            isOnline = true
        )
        database.chatDao().insertOrUpdateChat(newGroup)
        return groupId
    }

    suspend fun sendMessage(
        chatId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        mediaFile: File? = null,
        durationMs: Long = 0L,
        replyToId: String? = null,
        replyToText: String? = null
    ) {
        val userProfile = profileRepository.userProfileFlow.first()
        val messageId = UUID.randomUUID().toString()

        var mediaUriString: String? = null
        var mediaBase64: String? = null
        if (mediaFile != null && mediaFile.exists()) {
            mediaUriString = mediaFile.absolutePath
            val bytes = mediaFile.readBytes()
            mediaBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        val messageEntity = MessageEntity(
            messageId = messageId,
            chatId = chatId,
            senderAddress = userProfile.macAddress,
            senderName = userProfile.name,
            content = content,
            type = type.name,
            mediaUri = mediaUriString,
            durationMs = durationMs,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT.name,
            replyToId = replyToId,
            replyToText = replyToText,
            reactionsJson = "{}",
            isOutgoing = true
        )

        database.messageDao().insertMessage(messageEntity)
        database.chatDao().updateLastMessage(chatId, content.ifBlank { "[${type.name}]" }, System.currentTimeMillis())

        val chat = database.chatDao().getChatById(chatId) ?: return
        val targets = if (chat.type == "GROUP") {
            try { listAdapter.fromJson(chat.participantAddressesJson) ?: emptyList() } catch (_: Exception) { emptyList() }
        } else {
            listOf(chatId)
        }

        for (targetAddress in targets) {
            val packet = BluetoothPacket(
                type = "MESSAGE",
                packetId = UUID.randomUUID().toString(),
                senderAddress = userProfile.macAddress,
                senderName = userProfile.name,
                chatId = chatId,
                messageId = messageId,
                content = content,
                messageType = type.name,
                mediaData = mediaBase64,
                mediaFileName = mediaFile?.name,
                durationMs = durationMs,
                replyToId = replyToId,
                replyToText = replyToText,
                timestamp = System.currentTimeMillis()
            )

            val sent = bluetoothManager.sendPacket(targetAddress, packet)
            if (!sent) {
                // Queue for offline retry when target re-connects
                val packetJson = moshi.adapter(BluetoothPacket::class.java).toJson(packet)
                database.pendingMessageDao().enqueue(
                    PendingMessageEntity(
                        messageId = messageId,
                        targetAddress = targetAddress,
                        payloadJson = packetJson
                    )
                )
                database.messageDao().updateMessageStatus(messageId, MessageStatus.PENDING.name)
            }
        }
    }

    suspend fun addReaction(chatId: String, messageId: String, emoji: String) {
        val userProfile = profileRepository.userProfileFlow.first()
        val msg = database.messageDao().getMessageById(messageId) ?: return

        val currentReactions = try {
            mapAdapter.fromJson(msg.reactionsJson)?.toMutableMap() ?: mutableMapOf()
        } catch (_: Exception) {
            mutableMapOf()
        }

        currentReactions[userProfile.macAddress] = emoji
        val newJson = mapAdapter.toJson(currentReactions)
        database.messageDao().updateReactions(messageId, newJson)

        val packet = BluetoothPacket(
            type = "REACTION",
            senderAddress = userProfile.macAddress,
            senderName = userProfile.name,
            chatId = chatId,
            messageId = messageId,
            reactionEmoji = emoji
        )

        val chat = database.chatDao().getChatById(chatId) ?: return
        if (chat.type == "SINGLE") {
            bluetoothManager.sendPacket(chatId, packet)
        }
    }

    suspend fun markChatAsRead(chatId: String) {
        database.chatDao().resetUnreadCount(chatId)
        val userProfile = profileRepository.userProfileFlow.first()
        val packet = BluetoothPacket(
            type = "STATUS_ACK",
            senderAddress = userProfile.macAddress,
            senderName = userProfile.name,
            chatId = chatId,
            ackStatus = "READ"
        )
        bluetoothManager.sendPacket(chatId, packet)
    }

    private suspend fun handleIncomingPacket(packet: BluetoothPacket) {
        when (packet.type) {
            "MESSAGE" -> {
                var mediaLocalPath: String? = null
                if (!packet.mediaData.isNullOrBlank()) {
                    try {
                        val bytes = Base64.decode(packet.mediaData, Base64.NO_WRAP)
                        val fileName = packet.mediaFileName ?: "recv_${System.currentTimeMillis()}"
                        val file = File(context.cacheDir, fileName)
                        file.writeBytes(bytes)
                        mediaLocalPath = file.absolutePath
                    } catch (e: Exception) {
                        Log.e(tag, "Error saving incoming media payload", e)
                    }
                }

                val chatId = if (packet.chatId.startsWith("group_")) packet.chatId else packet.senderAddress

                // Ensure contact & chat exist
                val contact = ContactEntity(
                    address = packet.senderAddress,
                    name = packet.senderName,
                    isOnline = true
                )
                database.contactDao().insertOrUpdateContact(contact)

                val existingChat = database.chatDao().getChatById(chatId)
                if (existingChat == null) {
                    val newChat = ChatEntity(
                        chatId = chatId,
                        title = packet.senderName,
                        type = if (chatId.startsWith("group_")) "GROUP" else "SINGLE",
                        participantAddressesJson = listAdapter.toJson(listOf(packet.senderAddress)),
                        lastMessageText = packet.content,
                        lastMessageTime = packet.timestamp,
                        unreadCount = 1,
                        isOnline = true
                    )
                    database.chatDao().insertOrUpdateChat(newChat)
                } else {
                    database.chatDao().updateLastMessage(chatId, packet.content.ifBlank { "[${packet.messageType}]" }, packet.timestamp)
                    database.chatDao().incrementUnreadCount(chatId)
                }

                val incomingMsg = MessageEntity(
                    messageId = packet.messageId,
                    chatId = chatId,
                    senderAddress = packet.senderAddress,
                    senderName = packet.senderName,
                    content = packet.content,
                    type = packet.messageType,
                    mediaUri = mediaLocalPath,
                    durationMs = packet.durationMs,
                    timestamp = packet.timestamp,
                    status = MessageStatus.DELIVERED.name,
                    replyToId = packet.replyToId,
                    replyToText = packet.replyToText,
                    reactionsJson = "{}",
                    isOutgoing = false
                )
                database.messageDao().insertMessage(incomingMsg)

                // Send DELIVERED ACK back
                val ackPacket = BluetoothPacket(
                    type = "STATUS_ACK",
                    senderAddress = profileRepository.userProfileFlow.first().macAddress,
                    senderName = profileRepository.userProfileFlow.first().name,
                    chatId = chatId,
                    messageId = packet.messageId,
                    ackStatus = "DELIVERED"
                )
                bluetoothManager.sendPacket(packet.senderAddress, ackPacket)
            }

            "STATUS_ACK" -> {
                if (packet.messageId.isNotBlank() && packet.ackStatus != null) {
                    database.messageDao().updateMessageStatus(packet.messageId, packet.ackStatus)
                }
            }

            "REACTION" -> {
                if (packet.messageId.isNotBlank() && packet.reactionEmoji != null) {
                    val msg = database.messageDao().getMessageById(packet.messageId) ?: return
                    val reactionsMap = try {
                        mapAdapter.fromJson(msg.reactionsJson)?.toMutableMap() ?: mutableMapOf()
                    } catch (_: Exception) {
                        mutableMapOf()
                    }
                    reactionsMap[packet.senderAddress] = packet.reactionEmoji
                    database.messageDao().updateReactions(packet.messageId, mapAdapter.toJson(reactionsMap))
                }
            }
        }
    }

    private suspend fun flushPendingQueueForDevice(targetAddress: String) {
        val pendingList = database.pendingMessageDao().getPendingForDevice(targetAddress)
        for (item in pendingList) {
            val packet = try {
                moshi.adapter(BluetoothPacket::class.java).fromJson(item.payloadJson)
            } catch (_: Exception) {
                null
            }
            if (packet != null) {
                val sent = bluetoothManager.sendPacket(targetAddress, packet)
                if (sent) {
                    database.messageDao().updateMessageStatus(item.messageId, MessageStatus.SENT.name)
                    database.pendingMessageDao().dequeue(item.queueId)
                }
            }
        }
    }
}
