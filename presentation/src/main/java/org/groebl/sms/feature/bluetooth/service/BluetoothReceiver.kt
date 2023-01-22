package org.groebl.sms.feature.bluetooth.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import dagger.android.AndroidInjection
import org.groebl.sms.common.util.BluetoothHelper
import org.groebl.sms.feature.bluetooth.common.BluetoothTethering
import org.groebl.sms.util.Preferences
import javax.inject.Inject


class BluetoothReceiver : BroadcastReceiver() {

    @Inject lateinit var prefs: Preferences

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val btDeviceWhitelist =  prefs.bluetooth_devices.get()
        val btCurrentDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

        if (btDeviceWhitelist.contains(btCurrentDevice?.address)) {
            var deviceID = btCurrentDevice!!.address
            if (BluetoothHelper.hasBluetoothPermission(context)) {
                @SuppressLint("MissingPermission")
                if (btCurrentDevice.name != null) { deviceID = btCurrentDevice.name }
            }

            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    //Set Temp-Status to -Connected-
                    prefs.bluetooth_current_status.set(true)
                    prefs.bluetooth_last_connect.set(System.currentTimeMillis())
                    prefs.bluetooth_last_connect_device.set(deviceID)

                    //Set Bluetooth Tethering enabled
                    if (prefs.bluetooth_enabled.get() && prefs.bluetooth_tethering.get()) {
                        BluetoothTethering(context, context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).startTethering()
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (prefs.bluetooth_enabled.get()) {
                        //Set Bluetooth Tethering disabled
                        if (prefs.bluetooth_tethering.get()) {
                            BluetoothTethering(context, context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).stopTethering()
                        }

                        //Set Bluetooth Audio Volume back to normal
                        //if(mPrefs.getBoolean("bluetoothMaxVol", false) && mPrefs.getInt("bluetoothCurrentVol", -1) >= 0) {
                        //    val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        //    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mPrefs.getInt("bluetoothCurrentVol", (mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*0.5).toInt()), 0)
                        //}

                        //Delete Temporary Messages
                        val afterTimeDelete = if (prefs.bluetooth_only_on_connect.get() && prefs.bluetooth_autodelete.get()) 0L else 6L
                        Thread { BluetoothHelper.deleteBluetoothMessages(context, prefs.bluetooth_realm_hide_message.get(), afterTimeDelete) }.start()
                    }

                    //Set Temp-Status to -Disonnected-
                    prefs.bluetooth_current_status.set(false)
                    prefs.bluetooth_last_disconnect.set(System.currentTimeMillis())
                    prefs.bluetooth_last_connect_device.set(deviceID)
                    prefs.bluetooth_current_vol.set(-1)
                }

                BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1)
                    if (state == BluetoothA2dp.STATE_PLAYING) {
                        if (prefs.bluetooth_enabled.get() && prefs.bluetooth_max_vol.get() && prefs.bluetooth_current_vol.get() == -1) {
                            val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

                            Handler(Looper.getMainLooper()).postDelayed({
                                prefs.bluetooth_current_vol.set(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
                                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
                            }, 1000)
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
