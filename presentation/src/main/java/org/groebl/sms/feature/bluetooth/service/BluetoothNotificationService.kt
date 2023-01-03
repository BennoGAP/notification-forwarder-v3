package org.groebl.sms.feature.bluetooth.service

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.groebl.sms.common.util.BluetoothHelper
import org.groebl.sms.feature.bluetooth.common.BluetoothNotificationFilter.BT_Filter
import com.vdurmont.emoji.EmojiParser
import org.groebl.sms.common.util.AutoBluetoothNotificationManager
import org.groebl.sms.common.util.extensions.BluetoothMessageHelper
import timber.log.Timber
import java.lang.Exception
import java.util.HashSet

class BluetoothNotificationService : NotificationListenerService() {

    private lateinit var mBroadcastReceiver: BroadcastReceiver

    override fun onCreate() {
        super.onCreate()
        startMainService()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBroadcastReceiver)
    }

    private fun startMainService() {
        mBroadcastReceiver = BluetoothReceiver()
        val intentFilter = IntentFilter().apply {
            addAction("android.bluetooth.device.action.ACL_CONNECTED")
            addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED")
        }
        registerReceiver(mBroadcastReceiver, intentFilter)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        Timber.d("onNotificationPosted: ${sbn.packageName}")
        val mPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)


        //Check if Notification is -clearable-
        if (!sbn.isClearable) {
            return
        }

        //Check if Bluetooth-Fordward is enabled
        if (!mPrefs.getBoolean("bluetoothEnabled", false)) {
            return
        }

        //Check if Connected to bluetooth is enabled
        if (mPrefs.getBoolean("bluetoothOnlyOnConnect", true) && !mPrefs.getBoolean("bluetoothCurrentStatus", false)) {
            return
        }

        //Check if App is on App-Whitelist
        val pack = sbn.packageName
        val appwhitelist = mPrefs.getStringSet("bluetoothApps", HashSet())
        if (!appwhitelist!!.contains(pack)) {
            return
        }

        //Everything is fine - here we go..
        try {
            val BtData = BT_Filter()
            BtData.BluetoothFilter(sbn, applicationContext)

            //Ok, now save the Msg
            if (BtData.allData()) {

                //If text is empty - dont proceed
                val bluetoothText: String = BluetoothHelper.emojiToNiceEmoji(BtData.content, mPrefs.getBoolean("bluetoothEmoji", true))
                if (bluetoothText.trim().isEmpty()) { return }

                //Check if this msg already exist
                if (BluetoothHelper.isBluetoothHashCached(pack.toString(), BluetoothHelper.notificationHash(BtData.sender, BtData.content))) { return }

                //Enter the Data in the SMS-DB
                val btMessageHelper = BluetoothMessageHelper()
                btMessageHelper.addBluetoothMessage(applicationContext,
                    EmojiParser.removeAllEmojis(BtData.sender),
                    bluetoothText,
                    BtData.sendTime,
                    mPrefs.getBoolean("bluetoothRealmHideMessage", true),
                    mPrefs.getBoolean("bluetoothSaveRead", false) && !mPrefs.getBoolean("bluetoothDelayedRead", false),
                    BtData.errorCode,
                    mPrefs.getBoolean("canUseSubId", true),
                    0L
                )

                //Delayed Mark-as-Read
                if (mPrefs.getBoolean("bluetoothSaveRead", false) && mPrefs.getBoolean("bluetoothDelayedRead", false)) {
                    val cv = ContentValues()
                    cv.put(Telephony.Sms.READ, 1)
                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({
                        applicationContext.contentResolver.update(
                            Telephony.Sms.Inbox.CONTENT_URI,
                            cv,
                            Telephony.Sms.DATE_SENT + " = ? AND (" + Telephony.Sms.ERROR_CODE + " = ? OR " + Telephony.Sms.ERROR_CODE + " = ?)",
                            arrayOf(BtData.sendTime.toString(), "777", "778")
                        )
                    }, 500)
                }

                //Send Android-Auto Notification
                if (mPrefs.getBoolean("bluetoothAndroidAuto", false)) {
                    val mAutoNotificationManager = AutoBluetoothNotificationManager(
                        applicationContext
                    )
                    mAutoNotificationManager.notifyUser(
                        System.currentTimeMillis().toInt(),
                        if (BtData.hasPhoneNumber()) BtData.sender else BtData.appName,  //SINNVOLL? TODO
                        BtData.appNameRaw,
                        EmojiParser.removeAllEmojis(BtData.sender),
                        BluetoothHelper.emojiToNiceEmoji(BtData.contentRaw, mPrefs.getBoolean("bluetoothEmoji", true)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        Timber.d("onNotificationRemoved: ${sbn.packageName}")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}