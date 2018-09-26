package org.groebl.sms.feature.bluetooth.donate

data class BluetoothDonateState(
        val upgraded: Boolean = false,
        val upgradePrice1: String = "",
        val upgradePrice2: String = "",
        val upgradePrice3: String = "",
        val upgradePrice4: String = "",
        val currency: String = ""
)