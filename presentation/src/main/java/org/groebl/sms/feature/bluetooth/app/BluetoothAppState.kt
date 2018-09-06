package org.groebl.sms.feature.bluetooth.app

data class BluetoothAppState(
        val allowedApps: MutableSet<String>? = null
)