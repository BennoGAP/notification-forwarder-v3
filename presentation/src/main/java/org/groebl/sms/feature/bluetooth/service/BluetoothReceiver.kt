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

        if (bt_device_whitelist!!.contains(bt_device.address)) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    //Set Temp-Status to -Connected-
                    mPrefs.edit().putBoolean("bluetoothCurrentStatus", true).apply()
                    mPrefs.edit().putLong("bluetoothLastConnect", System.currentTimeMillis()).apply()


                    //Set Bluetooth Tethering
                    if (mPrefs.getBoolean("bluetoothTethering", false)) {
                        //Handler(Looper.getMainLooper()).postDelayed({
                        //TODO Tethering
                        //}, 5000)
                    }

                    //Set Bluetooth-Volume
                    if (mPrefs.getBoolean("bluetoothEnabled", false) && mPrefs.getBoolean("bluetoothMaxVol", false)) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            //Still connected?
                            if (mPrefs.getBoolean("bluetoothCurrentStatus", false)) {
                                val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
                            }
                        }, 7500)
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    //Set Temp-Status to -Disonnected-
                    mPrefs.edit().putBoolean("bluetoothCurrentStatus", false).apply()
                    mPrefs.edit().putLong("bluetoothLastConnect", 0L).apply()

                    //Disable BluetoothTethering
                    if (mPrefs.getBoolean("bluetoothTethering", false)) {
                        //Handler(Looper.getMainLooper()).postDelayed({
                        //TODO Tethering
                        //}, 5000)
                    }

                    //Delete Temporary Messages
                    if (mPrefs.getBoolean("bluetoothEnabled", false)) {
                        val afterTimeDelete = !(mPrefs.getBoolean("bluetoothOnlyOnConnect", true) && mPrefs.getBoolean("bluetoothAutodelete", true))
                        Thread {
                            BluetoothHelper.deleteBluetoothMessages(context, afterTimeDelete)
                        }.start()
                    }
                }
            }
        }
    }
}
