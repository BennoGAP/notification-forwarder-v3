package org.groebl.sms.common.util.extensions

import android.content.ContentValues
import android.content.Context
import android.provider.Telephony
import com.vdurmont.emoji.EmojiParser
import io.realm.Realm
import io.realm.Sort
import org.groebl.sms.compat.TelephonyCompat
import org.groebl.sms.model.BluetoothForwardCache
import org.groebl.sms.model.Conversation
import org.groebl.sms.model.Message
import org.groebl.sms.repository.SyncRepository
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class BluetoothMessageHelper {

    fun isBluetoothHashCached(app: String, hash: String): Boolean {
        val count = Realm.getDefaultInstance().use { realm ->
            realm.where(BluetoothForwardCache::class.java)
                .equalTo("app", app)
                .equalTo("hash", hash)
                .count()
        }

        //In Database, return true
        if (count > 0L) { return true; }

        var maxValue: Long = Realm.getDefaultInstance().use { realm -> realm.where(BluetoothForwardCache::class.java).max("id")?.toLong() ?: 0L }

        val bluetoothMessage = BluetoothForwardCache().apply {
            this.app = app
            this.hash = hash
            this.date = System.currentTimeMillis()
            this.id = ++maxValue
        }

        Realm.getDefaultInstance().executeTransaction { Realm.getDefaultInstance().insertOrUpdate(bluetoothMessage) }

        return false
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

    fun emojiToNiceEmoji(text: String, active: Boolean): String {
        /*
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
        */
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

    fun addBluetoothMessage(context: Context, address: String, body: String, sentTime: Long, hideInRealm: Boolean = true, asRead: Boolean = false, errorCode: Int, canUseSubId: Boolean = true, subId: Long = 0L, syncRepo: SyncRepository) {
        if(!hideInRealm) {
            addMessageToRealmInboxAsRead(context, address, body, sentTime, asRead, errorCode, canUseSubId, subId, syncRepo)
        } else {
            addMessageToInboxAsRead(context, address, body, sentTime, asRead, errorCode, canUseSubId, subId)
        }
    }

    private fun addMessageToInboxAsRead(context: Context, address: String, body: String, sentTime: Long, asRead: Boolean = false, errorCode: Int = 0, canUseSubId: Boolean = true, subId: Long = 0L) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE_SENT, sentTime)
            put(Telephony.Sms.SEEN, 1)
            put(Telephony.Sms.ERROR_CODE, errorCode)
        }

        if (canUseSubId) {
            values.put(Telephony.Sms.SUBSCRIPTION_ID, subId)
        }

        if(asRead) {
            values.put(Telephony.Sms.READ, 1)
        } else {
            values.put(Telephony.Sms.READ, 0)
        }

        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
    }

    private fun addMessageToRealmInboxAsRead(context: Context, address: String, body: String, sentTime: Long, asRead: Boolean = false, errorCode: Int = 0, canUseSubId: Boolean = true, subId: Long = 0L, syncRepo: SyncRepository) {
        var managedMessage: Message? = null

        var maxValue: Long = Realm.getDefaultInstance().use { realm -> realm.where(Message::class.java).max("id")?.toLong() ?: 0L }

        // Insert the message to Realm
        val message = Message().apply {
            this.address = address
            this.body = body
            this.dateSent = sentTime
            this.seen = true
            this.read = true
            this.errorCode = 0
            this.date = System.currentTimeMillis()
            this.subId = subId.toInt()
            this.isBluetoothMessage = true

            id = ++maxValue
            threadId = TelephonyCompat.getOrCreateThreadId(context, address)
            boxId = Telephony.Sms.MESSAGE_TYPE_INBOX
            type = "sms"
        }

        Realm.getDefaultInstance().executeTransaction { managedMessage = Realm.getDefaultInstance().copyToRealmOrUpdate(message) }

        // Insert the message to the native content provider
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE_SENT, sentTime)
            put(Telephony.Sms.SEEN, 1)
            put(Telephony.Sms.ERROR_CODE, errorCode)
        }

        if (canUseSubId) {
            values.put(Telephony.Sms.SUBSCRIPTION_ID, subId)
        }

        if (asRead) {
            values.put(Telephony.Sms.READ, 1)
        } else {
            values.put(Telephony.Sms.READ, 0)
        }

        val uri = context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)

        uri?.lastPathSegment?.toLong()?.let { id ->
            // Update the contentId after the message has been inserted to the content provider
            Realm.getDefaultInstance().executeTransaction { managedMessage?.contentId = id }
        }

        //Update Conversation/Thread
        val conversation = Realm.getDefaultInstance().use { realm -> realm
            .where(Conversation::class.java)
            .equalTo("id", message.threadId)
            .findFirst() }

        val lastMessage = Realm.getDefaultInstance().use { realm -> realm
            .where(Message::class.java)
            .equalTo("threadId", message.threadId)
            .sort("date", Sort.DESCENDING)
            .findFirst() }

        if (conversation == null) {
            syncRepo.syncMessage(uri!!)
            return
        } else {
            Realm.getDefaultInstance().executeTransaction { conversation.lastMessage = lastMessage }
        }
    }
}