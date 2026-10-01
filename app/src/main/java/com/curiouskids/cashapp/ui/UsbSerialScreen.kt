package com.curiouskids.cashapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.hardware.usb.UsbDevice

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow

@Composable
fun UsbSerialScreen(
    viewModel: UsbViewModel,
    onRequestPermission: (UsbDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val isDeviceAttached by viewModel.isDeviceAttached.collectAsState()
    val isBridgeConnected by viewModel.isBridgeConnected.collectAsState()
    val logs = viewModel.logs

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "USB Serial Logs", fontSize = 20.sp)
                val statusText = when {
                    isBridgeConnected -> "Status: Connected to Mac Hardware Bridge (Live)"
                    isConnected -> "Status: Connected (Real Hardware)"
                    isDeviceAttached -> "Status: Device Detected (Ready)"
                    else -> "Status: Ready to Connect Mac Hardware"
                }
                val statusColor = when {
                    isBridgeConnected || isConnected -> Color(0xFF4CAF50) // Green
                    isDeviceAttached -> Color(0xFF2196F3) // Blue
                    else -> Color(0xFFFF9800) // Orange
                }
                Text(text = statusText, color = statusColor, fontSize = 14.sp)
            }
            
            if (isConnected || isBridgeConnected) {
                Button(onClick = { viewModel.onDisconnectRequested() }) {
                    Text("Disconnect")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isDeviceAttached) {
                        Button(onClick = { viewModel.onConnectRequested(onRequestPermission) }) {
                            Text("Connect")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.onConnectBridgeRequested() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text("Connect Mac USB")
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(logs) { log ->
                Text(
                    text = log,
                    fontSize = 24.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
