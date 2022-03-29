package org.groebl.sms.feature.bluetooth.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import dagger.android.AndroidInjection
import org.groebl.sms.feature.bluetooth.common.BluetoothHelper
import org.groebl.sms.feature.bluetooth.common.BluetoothTethering


class BluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        val btDeviceWhitelist = mPrefs.getStringSet("bluetoothDevices", HashSet())
        val btCurrentDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

        if (btDeviceWhitelist!!.contains(btCurrentDevice?.address)) {
            var deviceID = btCurrentDevice!!.address
            if (BluetoothHelper.hasBluetoothPermission(context)) {
                @SuppressLint("MissingPermission")
                if (btCurrentDevice.name != null) { deviceID = btCurrentDevice.name }
            }

            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    //Set Temp-Status to -Connected-
                    mPrefs.edit().putBoolean("bluetoothCurrentStatus", true).apply()
                    mPrefs.edit().putLong("bluetoothLastConnect", System.currentTimeMillis()).apply()
                    mPrefs.edit().putString("bluetoothLastDevice", deviceID).apply()

                    //Set Bluetooth Tethering enabled
                    if (mPrefs.getBoolean("bluetoothEnabled", false) && mPrefs.getBoolean("bluetoothTethering", false)) {
                        BluetoothTethering(context, context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).startTethering()
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    //Set Temp-Status to -Disonnected-
                    mPrefs.edit().putBoolean("bluetoothCurrentStatus", false).apply()
                    mPrefs.edit().putLong("bluetoothLastDisconnect", System.currentTimeMillis()).apply()
                    mPrefs.edit().putString("bluetoothLastDevice", deviceID).apply()

                    //Set Bluetooth Tethering disabled
                    if (mPrefs.getBoolean("bluetoothEnabled", false) && mPrefs.getBoolean("bluetoothTethering", false)) {
                        BluetoothTethering(context, context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).stopTethering()
                    }

                    //Delete Temporary Messages
                    if (mPrefs.getBoolean("bluetoothEnabled", false)) {
                        val afterTimeDelete = !(mPrefs.getBoolean("bluetoothOnlyOnConnect", true) && mPrefs.getBoolean("bluetoothAutodelete", true))
                        Thread {
                            BluetoothHelper.deleteBluetoothMessages(context, afterTimeDelete)
                        }.start()
                    }
                }

                BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1)
                    if (state == BluetoothA2dp.STATE_PLAYING) {
                        if (mPrefs.getBoolean("bluetoothEnabled", false) && mPrefs.getBoolean("bluetoothMaxVol", false)) {
                            val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            mPrefs.edit().putInt("bluetoothCurrentVol", mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)).apply()
                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
                        }
                    } else if (state == BluetoothA2dp.STATE_NOT_PLAYING) {
                        if (mPrefs.getBoolean("bluetoothEnabled", false) && mPrefs.getBoolean("bluetoothMaxVol", false)) {
                            val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mPrefs.getInt("bluetoothCurrentVol", (mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*0.5).toInt()), 0)
                        }
                    }
                }

                /*
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1)
                    if (state == BluetoothA2dp.STATE_CONNECTED) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (mPrefs.getBoolean("bluetoothEnabled", false) && mPrefs.getBoolean("bluetoothMaxVol", false) && mPrefs.getBoolean("bluetoothCurrentStatus", false)) {
                                val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                mPrefs.edit().putInt("bluetoothCurrentVol", mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)).apply()
                                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
                            }
                        }, 1000)
                    } else if (state == BluetoothA2dp.STATE_DISCONNECTED) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (mPrefs.getBoolean("bluetoothEnabled", false) && mPrefs.getBoolean("bluetoothMaxVol", false) && !mPrefs.getBoolean("bluetoothCurrentStatus", false)) {
                                val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mPrefs.getInt("bluetoothCurrentVol", (mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*0.5).toInt()), 0)
                            }
                        }, 1000)
                    }
                }
                */

            }
        }
    }
}
