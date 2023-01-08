package org.groebl.sms.feature.bluetooth.service;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.vdurmont.emoji.EmojiParser;

import org.groebl.sms.common.util.extensions.BluetoothMessageHelper;
import org.groebl.sms.feature.bluetooth.common.BluetoothNotificationFilter;
import org.groebl.sms.repository.SyncRepository;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import dagger.android.AndroidInjection;


public class BluetoothNotificationService extends NotificationListenerService {

    @Inject
    SyncRepository syncRepo;

    private BroadcastReceiver mBroadcastReceiver;

    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();
        startMainService();
    }

    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mBroadcastReceiver);
    }

    public void startMainService() {
        this.mBroadcastReceiver = new BluetoothReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
        intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED");
        registerReceiver(this.mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        Log.d("Notification", "onNotificationPosted: " + sbn.getPackageName());

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //Check if Notification is -clearable-
        if (!sbn.isClearable()) { return; }

        //Check if Bluetooth-Fordward is enabled
        if (!mPrefs.getBoolean("bluetoothEnabled", false)) { return; }

        //Check if Connected to bluetooth is enabled
        if(mPrefs.getBoolean("bluetoothOnlyOnConnect", true) && !mPrefs.getBoolean("bluetoothCurrentStatus", false)) { return; }

        //Check if App is on App-Whitelist
        String pack = sbn.getPackageName();
        Set<String> appwhitelist = mPrefs.getStringSet("bluetoothApps", new HashSet<>());
        if (!appwhitelist.contains(pack)) { return; }

        //Everything is fine - here we go..
        try {
            BluetoothMessageHelper btMessageHelper = new BluetoothMessageHelper();
            BluetoothNotificationFilter.BT_Filter BtData = new BluetoothNotificationFilter.BT_Filter();
            BtData.BluetoothFilter(sbn, getApplicationContext());

            //Ok, now save the Msg
            if (BtData.allData()) {

                //Check if this msg already exist
                if (btMessageHelper.isBluetoothHashCached(pack, btMessageHelper.notificationHash(BtData.getSender(), BtData.getContent()))) { return; }

                //Enter the Data in the SMS-DB
                btMessageHelper.addBluetoothMessage(
                        getApplicationContext(),
                        EmojiParser.removeAllEmojis(BtData.getSender()),
                        btMessageHelper.emojiToNiceEmoji(BtData.getContent(), mPrefs.getBoolean("bluetoothEmoji", true)),
                        BtData.getSendTime(),
                        mPrefs.getBoolean("bluetoothRealmMessage", false),
                        (mPrefs.getBoolean("bluetoothSaveRead", false) && !mPrefs.getBoolean("bluetoothDelayedRead", false)),
                        BtData.getErrorCode(),
                        mPrefs.getBoolean("canUseSubId", true),
                        1,
                        syncRepo);

                //Delayed Mark-as-Read
                if (mPrefs.getBoolean("bluetoothSaveRead", false) && mPrefs.getBoolean("bluetoothDelayedRead", false)) {
                    ContentValues cv = new ContentValues();
                    cv.put(Telephony.Sms.READ, 1);

                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(() -> getApplicationContext().getContentResolver().update(Telephony.Sms.Inbox.CONTENT_URI, cv, Telephony.Sms.DATE_SENT + " = ? AND (" + Telephony.Sms.ERROR_CODE + " = ? OR " + Telephony.Sms.ERROR_CODE + " = ?)", new String[]{BtData.getSendTime().toString(), "777", "778"}), 500);
                }

            }

        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        Log.d("Notification", "onNotificationRemoved: " + sbn.getPackageName());
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

}