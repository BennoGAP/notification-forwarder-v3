package org.groebl.sms.feature.bluetooth.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import org.groebl.sms.feature.bluetooth.common.BluetoothHelper
import java.util.*

class BluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        val bt_device_whitelist = mPrefs.getStringSet("bluetoothDevices", HashSet())
        val bt_device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

        if (bt_device_whitelist!!.contains(bt_device.name)) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    //Set Temp-Status to -Connected-
                    mPrefs.edit().putBoolean("bluetoothCurrentStatus", true).apply()

                    //Set Bluetooth-Volume
                    if (mPrefs.getBoolean("bluetoothEnabled", false) && mPrefs.getBoolean("bluetoothMaxVol", false)) {
                        val handler = Handler(Looper.getMainLooper())
                        handler.postDelayed({
                            //Still connected?
                            if(mPrefs.getBoolean("bluetoothCurrentStatus", false)) {
                                val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
                            }
                        }, 7500)
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    //Set Temp-Status to -Disonnected-
                    mPrefs.edit().putBoolean("bluetoothCurrentStatus", false).apply()

                    //Delete Temporary Messages
                    if (mPrefs.getBoolean("bluetoothEnabled", false) && mPrefs.getBoolean("bluetoothOnlyOnConnect", true) && mPrefs.getBoolean("bluetoothAutodelete", true)) {
                        Thread {
                            BluetoothHelper.deleteBluetoothMessages(context, false)
                        }.start()
                    }
                }
            }
        }
    }
}
