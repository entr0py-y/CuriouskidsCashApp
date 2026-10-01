package com.curiouskids.cashapp.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.net.Socket

class UsbSerialManager(private val context: Context) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var usbSerialPort: UsbSerialPort? = null
    private var socket: Socket? = null
    private var readJob: Job? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _isDeviceAttached = MutableStateFlow(false)
    val isDeviceAttached = _isDeviceAttached.asStateFlow()

    private val _isBridgeConnected = MutableStateFlow(false)
    val isBridgeConnected = _isBridgeConnected.asStateFlow()

    private val _logs = MutableSharedFlow<String>(replay = 50)
    val logs = _logs.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    updateAttachmentStatus()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    // If the currently connected device is unplugged, disconnect it
                    if (usbSerialPort?.device?.deviceId == device?.deviceId) {
                        disconnect()
                    }
                    updateAttachmentStatus()
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        context.registerReceiver(usbReceiver, filter)
        updateAttachmentStatus()
    }

    private fun updateAttachmentStatus() {
        _isDeviceAttached.value = findDevice() != null
    }

    fun findDevice(): UsbDevice? {
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            Log.d("UsbSerialManager", "No USB serial devices found on Android USB stack.")
            return null
        }
        return availableDrivers[0].device
    }

    fun hasPermission(device: UsbDevice): Boolean {
        return usbManager.hasPermission(device)
    }

    fun connect(device: UsbDevice) {
        disconnect()
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
        if (driver == null) {
            Log.d("UsbSerialManager", "No driver found for device.")
            return
        }

        val connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            Log.d("UsbSerialManager", "Failed to open device connection.")
            return
        }

        val port = driver.ports[0]
        try {
            port.open(connection)
            port.setParameters(115200, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            usbSerialPort = port
            _isConnected.value = true
            Log.d("UsbSerialManager", "Connected to ${device.deviceName} at 115200 baud.")
            startReadingFromUsbPort()
        } catch (e: IOException) {
            Log.e("UsbSerialManager", "Error opening port: ${e.message}")
            try {
                port.close()
            } catch (_: IOException) {}
        }
    }

    fun connectToMacUsbBridge(host: String = "10.0.2.2", port: Int = 9999) {
        disconnect()
        readJob = scope.launch {
            try {
                Log.d("UsbSerialManager", "Connecting to Mac USB Serial Bridge at $host:$port...")
                val sock = Socket(host, port)
                socket = sock
                _isConnected.value = true
                _isBridgeConnected.value = true
                Log.d("UsbSerialManager", "Connected to Mac USB Serial Bridge!")

                val inputStream: InputStream = sock.getInputStream()
                val buffer = ByteArray(1024)
                val dataBuffer = StringBuilder()
                val regex = Regex("AA(\\d+)BB")

                while (sock.isConnected && !sock.isClosed) {
                    val len = inputStream.read(buffer)
                    if (len > 0) {
                        val data = String(buffer, 0, len)
                        Log.d("UsbSerialManager", "Bridge Raw Hardware Data: $data")
                        dataBuffer.append(data)

                        var match = regex.find(dataBuffer)
                        while (match != null) {
                            val value = match.groupValues[1]
                            emitValueToUi(value)
                            dataBuffer.delete(0, match.range.last + 1)
                            match = regex.find(dataBuffer)
                        }

                        if (dataBuffer.length > 2048) {
                            dataBuffer.delete(0, 1024)
                        }
                    } else if (len == -1) {
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("UsbSerialManager", "Error connecting to Mac USB bridge: ${e.message}")
            } finally {
                disconnect()
            }
        }
    }

    private fun startReadingFromUsbPort() {
        readJob?.cancel()
        readJob = scope.launch {
            val buffer = ByteArray(1024)
            val dataBuffer = StringBuilder()
            val regex = Regex("AA(\\d+)BB")

            while (usbSerialPort != null && usbSerialPort?.isOpen == true) {
                try {
                    val len = usbSerialPort?.read(buffer, 1000) ?: 0
                    if (len > 0) {
                        val data = String(buffer, 0, len)
                        Log.d("UsbSerialManager", "Raw Data: $data")
                        
                        dataBuffer.append(data)
                        
                        var match = regex.find(dataBuffer)
                        while (match != null) {
                            val value = match.groupValues[1]
                            emitValueToUi(value)
                            dataBuffer.delete(0, match.range.last + 1)
                            match = regex.find(dataBuffer)
                        }
                        
                        if (dataBuffer.length > 2048) {
                            dataBuffer.delete(0, 1024)
                        }
                    }
                } catch (e: IOException) {
                    Log.e("UsbSerialManager", "Read error: ${e.message}")
                    disconnect()
                    break
                }
                delay(100)
            }
        }
    }

    fun disconnect() {
        readJob?.cancel()
        readJob = null
        try {
            usbSerialPort?.close()
        } catch (e: IOException) {
            Log.e("UsbSerialManager", "Error closing port", e)
        }
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e("UsbSerialManager", "Error closing socket", e)
        }
        socket = null
        usbSerialPort = null
        _isConnected.value = false
        _isBridgeConnected.value = false
        Log.d("UsbSerialManager", "Disconnected.")
    }

    private fun emitValueToUi(value: String) {
        Log.d("UsbSerialManager", "Emitting to UI: $value")
        _logs.tryEmit(value)
    }
}
