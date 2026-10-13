package com.offiwp.app.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.offiwp.app.data.audio.AudioPlayer
import com.offiwp.app.data.audio.AudioRecorder
import com.offiwp.app.data.bluetooth.BluetoothManager
import com.offiwp.app.data.local.AppDatabase
import com.offiwp.app.data.repository.ChatRepository
import com.offiwp.app.data.repository.DeviceRepository
import com.offiwp.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NavTab {
    CHATS,
    NEARBY,
    PROFILE
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val database = AppDatabase.getDatabase(context)
    val bluetoothManager = BluetoothManager(context)

    val profileRepository = ProfileRepository(context)
    val deviceRepository = DeviceRepository(bluetoothManager)
    val chatRepository = ChatRepository(context, database, bluetoothManager, profileRepository)

    val audioRecorder = AudioRecorder(context)
    val audioPlayer = AudioPlayer(context)

    private val _selectedTab = MutableStateFlow(NavTab.CHATS)
    val selectedTab: StateFlow<NavTab> = _selectedTab.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    fun selectTab(tab: NavTab) {
        _selectedTab.value = tab
    }

    fun openChat(chatId: String) {
        _activeChatId.value = chatId
    }

    fun closeChat() {
        _activeChatId.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
        audioRecorder.cancelRecording()
        bluetoothManager.cleanUp()
    }
}
