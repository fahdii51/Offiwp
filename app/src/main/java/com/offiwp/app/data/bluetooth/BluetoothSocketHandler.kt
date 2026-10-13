package com.offiwp.app.data.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.offiwp.app.data.crypto.EncryptionHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter

class BluetoothSocketHandler(
    val socket: BluetoothSocket,
    val deviceAddress: String,
    private val onDisconnected: (address: String) -> Unit
) {

    private val tag = "BluetoothSocketHandler"
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(BluetoothPacket::class.java)

    private val _incomingPackets = MutableSharedFlow<BluetoothPacket>()
    val incomingPackets: SharedFlow<BluetoothPacket> = _incomingPackets.asSharedFlow()

    private var readerThread: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private var writer: PrintWriter? = null
    private var isConnected = false

    fun startListening() {
        isConnected = true
        readerThread = scope.launch {
            try {
                writer = PrintWriter(socket.outputStream, true)
                val inputStreamReader = InputStreamReader(socket.inputStream, Charsets.UTF_8)
                val bufferedReader = BufferedReader(inputStreamReader)

                while (isConnected && socket.isConnected) {
                    val encryptedLine = bufferedReader.readLine() ?: break
                    if (encryptedLine.isBlank()) continue

                    val decryptedJson = EncryptionHelper.decrypt(encryptedLine)
                    val packet = adapter.fromJson(decryptedJson)
                    if (packet != null) {
                        _incomingPackets.emit(packet)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Socket read error for device $deviceAddress", e)
            } finally {
                closeSilently()
                onDisconnected(deviceAddress)
            }
        }
    }

    fun sendPacket(packet: BluetoothPacket): Boolean {
        return try {
            val json = adapter.toJson(packet)
            val encrypted = EncryptionHelper.encrypt(json)
            writer?.println(encrypted)
            writer?.flush()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error sending packet to $deviceAddress", e)
            closeSilently()
            onDisconnected(deviceAddress)
            false
        }
    }

    fun closeSilently() {
        isConnected = false
        readerThread?.cancel()
        try {
            writer?.close()
            socket.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing socket for $deviceAddress", e)
        }
    }
}
