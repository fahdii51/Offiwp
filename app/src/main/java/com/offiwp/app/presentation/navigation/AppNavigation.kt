package com.offiwp.app.presentation.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offiwp.app.presentation.MainViewModel
import com.offiwp.app.presentation.NavTab
import com.offiwp.app.presentation.chats.ChatDetailScreen
import com.offiwp.app.presentation.chats.ChatListScreen
import com.offiwp.app.presentation.devices.NearbyDevicesScreen
import com.offiwp.app.presentation.profile.ProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val activeChatId by viewModel.activeChatId.collectAsState()

    if (activeChatId != null) {
        ChatDetailScreen(
            chatId = activeChatId!!,
            viewModel = viewModel,
            onBackClick = { viewModel.closeChat() }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Offi Wp",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Bluetooth P2P",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.deviceRepository.startScanning() },
                            modifier = Modifier.testTag("appbar_refresh_scan")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Scan Bluetooth")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    NavigationBarItem(
                        selected = selectedTab == NavTab.CHATS,
                        onClick = { viewModel.selectTab(NavTab.CHATS) },
                        icon = { Icon(Icons.Filled.ChatBubble, contentDescription = "Chats") },
                        label = { Text("Chats") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_chats")
                    )

                    NavigationBarItem(
                        selected = selectedTab == NavTab.NEARBY,
                        onClick = { viewModel.selectTab(NavTab.NEARBY) },
                        icon = { Icon(Icons.Filled.Bluetooth, contentDescription = "Nearby") },
                        label = { Text("Nearby") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_nearby")
                    )

                    NavigationBarItem(
                        selected = selectedTab == NavTab.PROFILE,
                        onClick = { viewModel.selectTab(NavTab.PROFILE) },
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_profile")
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Crossfade(targetState = selectedTab, label = "tab_fade") { tab ->
                    when (tab) {
                        NavTab.CHATS -> ChatListScreen(
                            viewModel = viewModel,
                            onChatClick = { chatId -> viewModel.openChat(chatId) },
                            onNavigateToNearby = { viewModel.selectTab(NavTab.NEARBY) }
                        )
                        NavTab.NEARBY -> NearbyDevicesScreen(
                            viewModel = viewModel,
                            onDeviceConnected = { address -> viewModel.openChat(address) }
                        )
                        NavTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
