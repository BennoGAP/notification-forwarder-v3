package org.groebl.sms.common.util

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vdurmont.emoji.EmojiParser
import io.realm.Realm
import io.realm.Sort
import org.groebl.sms.feature.bluetooth.service.BluetoothNotificationService
import org.groebl.sms.model.BluetoothForwardCache
import org.groebl.sms.model.Conversation
import org.groebl.sms.model.Message
import timber.log.Timber
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit


object BluetoothHelper {

    fun emojiToNiceEmoji(text: String, active: Boolean): String {
        var text = text
        /*
        text = text.replace("[\ud83d\ude42]".toRegex(), ":)")
        text = text.replace("[\ud83d\ude0a]".toRegex(), ":)")
        text = text.replace("[\ud83d\ude09]".toRegex(), ";)")
        text = text.replace("[\ud83d\ude00]".toRegex(), ":D")
        text = text.replace("[\ud83d\ude03]".toRegex(), ":D")
        text = text.replace("[\ud83d\ude04]".toRegex(), ":D")
        text = text.replace("[\ud83d\ude2c]".toRegex(), "=D")
        text = text.replace("[\ud83d\ude01]".toRegex(), "=D")
        text = text.replace("[\ud83d\ude0b]".toRegex(), ":P")
        text = text.replace("[\ud83d\ude1b]".toRegex(), ":P")
        text = text.replace("[\ud83d\ude1c]".toRegex(), ";P")
        text = text.replace("[\ud83d\ude1d]".toRegex(), ";P")
        text = text.replace("[\ud83d\ude41]".toRegex(), ":(")
        text = text.replace("[\u2639]".toRegex(), ":(")
        text = text.replace("[\ud83d\ude10]".toRegex(), ":|")
        text = text.replace("[\ud83d\ude11]".toRegex(), ":|")
        */
        text = text.replace("[\ud83d\udc9a]".toRegex(), "<3")
        text = text.replace("[\ud83d\udc9b]".toRegex(), "<3")
        text = text.replace("[\ud83d\udc9c]".toRegex(), "<3")
        text = text.replace("[\ud83d\udc99]".toRegex(), "<3")
        text = text.replace("[\u2764]".toRegex(), "<3")
        text = text.replace("[\ud83d\udc94]".toRegex(), "</3")
        //TODO: find more

        //text = text.replaceAll("[\u25a1]", ""); // [] =>
        /*
            String output = "";
            for (int i = 0; i < text.length(); i++) {
                output = output + ":" + Integer.toHexString(text.charAt(i));
            }
            Log.d("output", output);
        */

        return if (active) EmojiParser.parseToAliases(text, EmojiParser.FitzpatrickAction.REMOVE) else text
    }

    fun notificationHash(sender: String, content: String): String {
        val code = "$content | $sender"

        try {
            val m = MessageDigest.getInstance("MD5")
            m.update(code.toByteArray(), 0, code.length)
            val big = BigInteger(1, m.digest())
            return String.format("%1$032x", big)
        } catch (e: NoSuchAlgorithmException) {
            return code.substring(0, 31)
        }
    }

    private fun toggleNotificationListenerService(context: Context) {
        val componentName = ComponentName(context, BluetoothNotificationService::class.java)
        val packageManager = context.packageManager
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
    }

    fun hasNotificationAccess(context: Context): Boolean {
        val enabledNotificationListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return enabledNotificationListeners != null && enabledNotificationListeners.contains(context.packageName, ignoreCase = true) && enabledNotificationListeners.contains(".service.BluetoothNotificationService", ignoreCase = true)
    }

    fun isNotificationServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if(service.service.className == "org.groebl.sms.feature.bluetooth.service.BluetoothNotificationService") {
                return true
            }
        }

        return false
    }

    fun checkAndRestartNotificationListener(context: Context) {
        if(hasNotificationAccess(context) && !isNotificationServiceRunning(context)) {
            toggleNotificationListenerService(context)
            try {
                //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //    context.startForegroundService(Intent(context, BluetoothNotificationService::class.java))
                //} else {
                    context.startService(Intent(context, BluetoothNotificationService::class.java))
                //}
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun isDefaultSms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
    }

    fun hasContactPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    fun requestContactPermission(activity: Activity) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_CONTACTS), 0)
    }

    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun requestBluetoothPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 0)
        }
    }

    fun deleteBluetoothMessages(context: Context, hideInRealm: Boolean = true, afterTime: Long = 0L) {
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()

            //Delete from Realm
            val messages = realm.where(Message::class.java)
                    .equalTo("isBluetoothMessage", true)
                    .let { if (afterTime > 0L) it.lessThanOrEqualTo("date", System.currentTimeMillis() - TimeUnit.HOURS.toMillis(afterTime)) else it }
                    .findAll()

            val updateIds = HashSet(messages.map { it.threadId })

            realm.executeTransaction { messages.deleteAllFromRealm() }

            //Update Realm to show latest Message in Preview
            updateIds.forEach { threadId ->
                val conversation = realm.where(Conversation::class.java)
                        .equalTo("id", threadId)
                        .findFirst() ?: return

                val message = realm.where(Message::class.java)
                        .equalTo("threadId", threadId)
                        .sort("date", Sort.DESCENDING)
                        .let { if (hideInRealm) it.notEqualTo("isBluetoothMessage", true) else it }
                        .findFirst()

                realm.executeTransaction {
                    conversation.lastMessage = message
                }
            }

            //Delete from Cache
            val cacheMessages = realm.where(BluetoothForwardCache::class.java)
                .let { if (afterTime > 0L) it.lessThanOrEqualTo("date", System.currentTimeMillis() - TimeUnit.HOURS.toMillis(afterTime)) else it }
                .findAll()

            realm.executeTransaction { cacheMessages.deleteAllFromRealm() }
        }

        //Delete from ContentResolver
        val selection: String = when {
            (afterTime > 0L)    ->  " AND date_sent <= " + (System.currentTimeMillis() - TimeUnit.HOURS.toMillis(afterTime))
            else                ->  ""
        }

        try {
            context.contentResolver.delete(Telephony.Sms.CONTENT_URI, "(" + Telephony.Sms.ERROR_CODE + " = ? or " + Telephony.Sms.ERROR_CODE + " = ?)" + selection, arrayOf("777", "778"))
        } catch (e: Exception) {
            Timber.e(e)
        }
    }


    fun getDontKillMyAppUrl(appName: String): String {
        return when (Build.MANUFACTURER) {
            "Xiaomi" -> "https://dontkillmyapp.com/xiaomi$appName"
            "Nokia" -> "https://dontkillmyapp.com/nokia$appName"
            "OnePlus" -> "https://dontkillmyapp.com/oneplus$appName"
            "Huawei" -> "https://dontkillmyapp.com/huawei$appName"
            "Meizu" -> "https://dontkillmyapp.com/meizu$appName"
            "Samsung" -> "https://dontkillmyapp.com/samsung$appName"
            "Sony" -> "https://dontkillmyapp.com/sony$appName"
            "HTC" -> "https://dontkillmyapp.com/htc$appName"
            "Google" -> "https://dontkillmyapp.com/stock_android$appName"
            "Lenovo" -> "https://dontkillmyapp.com/lenovo$appName"
            else -> "https://dontkillmyapp.com/$appName"
        }
    }

    fun isBluetoothHashCached(app: String, hash: String): Boolean {
        val realm = Realm.getDefaultInstance()
        realm.refresh()

        val count = realm.where(BluetoothForwardCache::class.java)
            .equalTo("app", app)
            .equalTo("hash", hash)
            .count()

        //In Database, return true
        if (count > 0L) { return true; }

        var maxValue: Long = realm.use { realm ->
            realm.where(BluetoothForwardCache::class.java).max("id")?.toLong() ?: 0L
        }

        val bluetoothMessage = BluetoothForwardCache().apply {
            this.app = app
            this.hash = hash
            this.date = System.currentTimeMillis()
            this.id = ++maxValue
        }

        realm.executeTransaction { realm.insert(bluetoothMessage) }
        return false
    }

    fun findWhatsAppNameFromNumber(context: Context, number: String): String {
        var setName = ""
        /*
        val country = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0).country
        } else {
            context.resources.configuration.locale.country
        }
        PhoneNumberUtils.formatNumberToE164(PhoneNumberUtils.stripSeparators(number), country)
        */
        try {
            val c = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                ContactsContract.CommonDataKinds.Phone.NUMBER + " = ? AND account_type = ?",
                arrayOf(PhoneNumberUtils.stripSeparators(number), "com.whatsapp"), null
            )


            c.use { c ->
                if ((c != null) && c.moveToFirst()) {
                    setName = c.getString(0)
                }
            }
        } catch (e: Exception) {
            Timber.w(e)
        }

        return setName
    }
}