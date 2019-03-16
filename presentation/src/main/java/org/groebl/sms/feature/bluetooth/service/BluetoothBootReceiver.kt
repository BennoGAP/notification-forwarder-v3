package org.groebl.sms.feature.bluetooth.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.android.AndroidInjection
import org.groebl.sms.feature.bluetooth.common.BluetoothHelper

class BluetoothBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        AndroidInjection.inject(this, context)

        BluetoothHelper.checkAndRestartNotificationListener(context)
    }
}
