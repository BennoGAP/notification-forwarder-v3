package org.groebl.sms.feature.bluetooth

data class BluetoothSettingsState(
        val bluetooth_enabled: Boolean = false,
        val bluetooth_only_on_connect: Boolean = true,
        val bluetooth_autodelete: Boolean = true,

        val bluetooth_save_read: Boolean = false,
        val bluetooth_delayed_read: Boolean = false,
        val bluetooth_emoji: Boolean = true,
        val bluetooth_appname_as_sender_text: Boolean = false,
        val bluetooth_appname_as_sender_number: Boolean = false,

        val bluetooth_whatsapp_to_contact: Boolean = true,
        val bluetooth_whatsapp_hide_prefix: Boolean = true,

        val bluetooth_max_vol: Boolean = false,
        val bluetooth_tethering: Boolean = false
)