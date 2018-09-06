package org.groebl.sms.feature.bluetooth.device

data class BluetoothDeviceState(
        val allowedDevices: HashSet<String>? = null
)