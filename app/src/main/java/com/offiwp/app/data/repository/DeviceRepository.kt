package com.offiwp.app.data.repository

import com.offiwp.app.data.bluetooth.BluetoothManager
import com.offiwp.app.domain.model.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class DeviceRepository(private val bluetoothManager: BluetoothManager) {

    val scannedDevices: StateFlow<Map<String, Device>> = bluetoothManager.scannedDevices
    val connectedAddresses: StateFlow<Set<String>> = bluetoothManager.connectedAddresses
    val isScanning: StateFlow<Boolean> = bluetoothManager.isScanning

    fun isBluetoothEnabled(): Boolean = bluetoothManager.isBluetoothEnabled()

    fun startScanning() {
        bluetoothManager.startDiscovery()
    }

    fun stopScanning() {
        bluetoothManager.stopDiscovery()
    }

    fun connectToDevice(address: String, onResult: (Boolean) -> Unit) {
        bluetoothManager.connectToDevice(address, onResult)
    }

    fun disconnectDevice(address: String) {
        bluetoothManager.disconnectDevice(address)
    }
}
