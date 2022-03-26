package org.groebl.sms.feature.bluetooth.service

import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import dagger.android.AndroidInjection
import org.groebl.sms.feature.bluetooth.common.BluetoothHelper
import org.groebl.sms.feature.bluetooth.common.BluetoothPanHelper
import java.util.*

class BluetoothReceiver : BroadcastReceiver() {

    private var mProfileListener: BluetoothProfile.ServiceListener? = null
    private var mBluetoothPanHelper: BluetoothPanHelper? = null

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        val btDeviceWhitelist = mPrefs.getStringSet("bluetoothDevices", HashSet())
        val btCurrentDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

        if (btDeviceWhitelist!!.contains(btCurrentDevice?.address)) {
            val deviceID = if (btCurrentDevice?.name != null) btCurrentDevice.name else btCurrentDevice!!.address

            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    //Set Temp-Status to -Connected-
                    mPrefs.edit().putBoolean("bluetoothCurrentStatus", true).apply()
                    mPrefs.edit().putLong("bluetoothLastConnect", System.currentTimeMillis()).apply()
                    mPrefs.edit().putString("bluetoothLastDevice", deviceID).apply()


                    //Set Bluetooth Tethering
                    if (mPrefs.getBoolean("bluetoothEnabled", false) && mPrefs.getBoolean("bluetoothTethering", false)) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                                val adapter = bluetoothManager.adapter

                                if (adapter != null) {
                                    mProfileListener = object : BluetoothProfile.ServiceListener {
                                        var myproxy: BluetoothProfile? = null
                                        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                                            myproxy = proxy
                                            mBluetoothPanHelper = BluetoothPanHelper(proxy)
                                            mBluetoothPanHelper?.setBluetoothTethering(true)
                                        }

                                        override fun onServiceDisconnected(profile: Int) {
                                            adapter.closeProfileProxy(profile, myproxy)
                                        }
                                    }

                                    adapter.getProfileProxy(context, mProfileListener, 5)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, 1000)
                    }

                }

                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    if (intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1) == BluetoothA2dp.STATE_CONNECTED) {

                        Handler(Looper.getMainLooper()).postDelayed({
                            if (mPrefs.getBoolean("bluetoothEnabled", false) && mPrefs.getBoolean("bluetoothMaxVol", false) && mPrefs.getBoolean("bluetoothCurrentStatus", false)) {
                                val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
                            }
                        }, 1000)
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    //Set Temp-Status to -Disonnected-
                    mPrefs.edit().putBoolean("bluetoothCurrentStatus", false).apply()
                    mPrefs.edit().putLong("bluetoothLastDisconnect", System.currentTimeMillis()).apply()
                    mPrefs.edit().putString("bluetoothLastDevice", deviceID).apply()

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
