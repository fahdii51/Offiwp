package com.offiwp.app.domain.model

data class Device(
    val name: String,
    val address: String,
    val rssi: Int = -70,
    val bondState: Int = 10, // BluetoothDevice.BOND_NONE = 10, BOND_BONDED = 12
    val isConnected: Boolean = false,
    val isOnline: Boolean = false
)
