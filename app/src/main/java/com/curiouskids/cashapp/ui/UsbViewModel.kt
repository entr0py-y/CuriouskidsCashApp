package com.curiouskids.cashapp.ui

import android.hardware.usb.UsbDevice
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.curiouskids.cashapp.usb.UsbSerialManager

@HiltViewModel
class UsbViewModel @Inject constructor(
    private val usbSerialManager: UsbSerialManager
) : ViewModel() {

    val logs = mutableStateListOf<String>()
    val isConnected: StateFlow<Boolean> = usbSerialManager.isConnected
    val isDeviceAttached: StateFlow<Boolean> = usbSerialManager.isDeviceAttached
    val isBridgeConnected: StateFlow<Boolean> = usbSerialManager.isBridgeConnected

    init {
        viewModelScope.launch {
            usbSerialManager.logs.collect { message ->
                // Ensure we update the state on the Main thread for Compose
                launch(Main) {
                    addLog(message)
                }
            }
        }
    }

    fun onConnectRequested(onPermissionRequired: (UsbDevice) -> Unit) {
        val device = usbSerialManager.findDevice()
        if (device != null) {
            if (usbSerialManager.hasPermission(device)) {
                usbSerialManager.connect(device)
            } else {
                onPermissionRequired(device)
            }
        } else {
            // Connect to Mac USB Hardware Bridge (for Android Studio Emulator)
            usbSerialManager.connectToMacUsbBridge()
        }
    }

    fun onConnectBridgeRequested() {
        usbSerialManager.connectToMacUsbBridge()
    }

    fun onPermissionGranted(device: UsbDevice) {
        usbSerialManager.connect(device)
    }

    fun onDisconnectRequested() {
        usbSerialManager.disconnect()
    }

    private fun addLog(message: String) {
        Log.d("UsbViewModel","${System.currentTimeMillis() % 100000}: $message")

        logs.add(0, "$message ₹")
        if (logs.size > 200) logs.removeAt(logs.size - 1)
    }
}
