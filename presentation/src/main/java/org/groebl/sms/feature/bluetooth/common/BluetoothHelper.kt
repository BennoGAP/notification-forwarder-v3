package org.groebl.sms.feature.bluetooth.common

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vdurmont.emoji.EmojiParser
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object BluetoothHelper  {

    fun emojiToNiceEmoji(text: String, active: Boolean): String {
        var text = text
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

    fun addMessageToInboxAsRead(context: Context, address: String, body: String, sentTime: Long, asRead: Boolean, errorCode: Int) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE_SENT, sentTime)
            put(Telephony.Sms.SEEN, true)
            put(Telephony.Sms.ERROR_CODE, errorCode)
            if (asRead) { put(Telephony.Sms.READ, true) }
    }

        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
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

    fun hasNotificationAccess(context: Context): Boolean {
        val enabledNotificationListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return enabledNotificationListeners != null && enabledNotificationListeners.contains("org.groebl.sms") && enabledNotificationListeners.contains("feature.bluetooth.service.BluetoothNotificationService")
    }

    fun hasContactPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    fun requestContactPermission(activity: Activity) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_CONTACTS), 0)
    }

    fun deleteBluetoothMessages(context: Context, afterTime: Boolean) {
        val selection: String = when {
            afterTime ->    " AND date_sent < " + (System.currentTimeMillis() - 21600000)
            else ->         ""
        }

        context.contentResolver.delete(Telephony.Sms.CONTENT_URI, "(" + Telephony.Sms.ERROR_CODE + " = ? or " + Telephony.Sms.ERROR_CODE + " = ?)" + selection, arrayOf("777", "778"))
    }

    fun findWhatsAppNumberFromName(context: Context, displayname: String): String {
        var setNumber = ""
        val c = context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DATA1),
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " = ? AND account_type = ?",
                arrayOf(displayname, "com.whatsapp"), null)

        if (c != null) {
            if (c.moveToFirst())  { setNumber = c.getString(0) }
            if (!c.isClosed)      { c.close() }
        }

        return setNumber
    }

    fun findWhatsAppNameFromNumber(context: Context, number: String): String {
        var setName = ""
        val c = context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                ContactsContract.CommonDataKinds.Phone.NUMBER + " = ? AND account_type = ?",
                arrayOf(PhoneNumberUtils.stripSeparators(number), "com.whatsapp"), null)

        if (c != null) {
            if (c.moveToFirst())  { setName = c.getString(0) }
            if (!c.isClosed)      { c.close() }
        }

        return setName
    }

}