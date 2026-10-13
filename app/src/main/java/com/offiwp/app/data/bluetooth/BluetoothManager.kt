package com.offiwp.app.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager as SystemBluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.offiwp.app.domain.model.Device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages Bluetooth Classic & BLE device discovery, incoming server socket listener,
 * client socket connections, and real-time P2P packet dispatch.
 */
@SuppressLint("MissingPermission")
class BluetoothManager(private val context: Context) {

    private val tag = "BluetoothManager"
    val appUuid: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

    private val systemBtManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? SystemBluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = systemBtManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

    private val _scannedDevices = MutableStateFlow<Map<String, Device>>(emptyMap())
    val scannedDevices: StateFlow<Map<String, Device>> = _scannedDevices.asStateFlow()

    private val _connectedAddresses = MutableStateFlow<Set<String>>(emptySet())
    val connectedAddresses: StateFlow<Set<String>> = _connectedAddresses.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isServerListening = MutableStateFlow(false)
    val isServerListening: StateFlow<Boolean> = _isServerListening.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<BluetoothPacket>()
    val incomingPackets: SharedFlow<BluetoothPacket> = _incomingPackets.asSharedFlow()

    private val activeHandlers = ConcurrentHashMap<String, BluetoothSocketHandler>()
    private var serverSocketThread: Job? = null
    private var autoReconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi: Int = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (-70).toShort()).toInt()

                    device?.let {
                        val address = it.address ?: return
                        val name = it.name ?: "Nearby Offi Device"
                        val bondState = it.bondState
                        val isConn = activeHandlers.containsKey(address)

                        _scannedDevices.value = _scannedDevices.value.toMutableMap().apply {
                            put(address, Device(
                                name = name,
                                address = address,
                                rssi = rssi,
                                bondState = bondState,
                                isConnected = isConn,
                                isOnline = isConn
                            ))
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON) {
                        startServerSocket()
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
        startServerSocket()
        startAutoReconnectTask()
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    /**
     * Starts device discovery for nearby Bluetooth devices.
     */
    fun startDiscovery() {
        if (!isBluetoothEnabled()) return
        bluetoothAdapter?.let { adapter ->
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            _isScanning.value = true
            addPairedDevicesToScannedList()
            adapter.startDiscovery()
        }
    }

    fun stopDiscovery() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }
        _isScanning.value = false
    }

    private fun addPairedDevicesToScannedList() {
        val bondedDevices = bluetoothAdapter?.bondedDevices ?: return
        val currentMap = _scannedDevices.value.toMutableMap()
        for (device in bondedDevices) {
            val address = device.address ?: continue
            val isConn = activeHandlers.containsKey(address)
            if (!currentMap.containsKey(address)) {
                currentMap[address] = Device(
                    name = device.name ?: "Paired Device",
                    address = address,
                    rssi = -60,
                    bondState = device.bondState,
                    isConnected = isConn,
                    isOnline = isConn
                )
            }
        }
        _scannedDevices.value = currentMap
    }

    /**
     * Starts background Bluetooth Server Socket listening for incoming P2P connections.
     */
    fun startServerSocket() {
        if (!isBluetoothEnabled() || serverSocketThread?.isActive == true) return

        serverSocketThread = scope.launch {
            _isServerListening.value = true
            var serverSocket: BluetoothServerSocket? = null
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("Offi Wp P2P", appUuid)
                while (isBluetoothEnabled()) {
                    val socket = serverSocket?.accept() ?: break
                    handleNewSocketConnection(socket)
                }
            } catch (e: Exception) {
                Log.e(tag, "Server socket accept loop error", e)
            } finally {
                _isServerListening.value = false
                try {
                    serverSocket?.close()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Connects to a target nearby device via client socket.
     */
    fun connectToDevice(address: String, onResult: (Boolean) -> Unit) {
        if (!isBluetoothEnabled()) {
            onResult(false)
            return
        }

        if (activeHandlers.containsKey(address)) {
            onResult(true)
            return
        }

        scope.launch {
            try {
                stopDiscovery()
                val targetDevice = bluetoothAdapter?.getRemoteDevice(address)
                if (targetDevice == null) {
                    onResult(false)
                    return@launch
                }

                val socket = targetDevice.createRfcommSocketToServiceRecord(appUuid)
                socket.connect()

                handleNewSocketConnection(socket)
                onResult(true)
            } catch (e: Exception) {
                Log.e(tag, "Failed to connect to $address", e)
                onResult(false)
            }
        }
    }

    private fun handleNewSocketConnection(socket: BluetoothSocket) {
        val remoteAddress = socket.remoteDevice?.address ?: return
        val handler = BluetoothSocketHandler(
            socket = socket,
            deviceAddress = remoteAddress,
            onDisconnected = { addr ->
                activeHandlers.remove(addr)
                updateConnectionState(addr, isConnected = false)
            }
        )

        activeHandlers[remoteAddress] = handler
        updateConnectionState(remoteAddress, isConnected = true)

        scope.launch {
            handler.incomingPackets.collect { packet ->
                _incomingPackets.emit(packet)
            }
        }

        handler.startListening()
    }

    private fun updateConnectionState(address: String, isConnected: Boolean) {
        val currentSet = _connectedAddresses.value.toMutableSet()
        if (isConnected) currentSet.add(address) else currentSet.remove(address)
        _connectedAddresses.value = currentSet

        _scannedDevices.value = _scannedDevices.value.toMutableMap().apply {
            get(address)?.let { old ->
                put(address, old.copy(isConnected = isConnected, isOnline = isConnected))
            }
        }
    }

    /**
     * Sends a packet to a connected device socket.
     */
    fun sendPacket(targetAddress: String, packet: BluetoothPacket): Boolean {
        val handler = activeHandlers[targetAddress]
        return handler?.sendPacket(packet) ?: false
    }

    /**
     * Auto-reconnect loop for previously paired devices.
     */
    private fun startAutoReconnectTask() {
        autoReconnectJob?.cancel()
        autoReconnectJob = scope.launch {
            while (true) {
                delay(15000) // Check every 15s
                if (isBluetoothEnabled()) {
                    val bonded = bluetoothAdapter?.bondedDevices ?: emptySet()
                    for (device in bonded) {
                        val addr = device.address ?: continue
                        if (!activeHandlers.containsKey(addr)) {
                            connectToDevice(addr) { success ->
                                if (success) Log.d(tag, "Auto-reconnected to $addr")
                            }
                        }
                    }
                }
            }
        }
    }

    fun disconnectDevice(address: String) {
        activeHandlers[address]?.closeSilently()
        activeHandlers.remove(address)
        updateConnectionState(address, isConnected = false)
    }

    fun cleanUp() {
        autoReconnectJob?.cancel()
        serverSocketThread?.cancel()
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (_: Exception) {}
        activeHandlers.values.forEach { it.closeSilently() }
        activeHandlers.clear()
    }
}
