package org.groebl.sms.common.util.extensions

import android.content.ContentValues
import android.content.Context
import android.provider.Telephony
import io.realm.Realm
import io.realm.Sort
import org.groebl.sms.compat.TelephonyCompat
import org.groebl.sms.model.Conversation
import org.groebl.sms.model.Message

class BluetoothMessageHelper {

    fun addBluetoothMessage(context: Context, address: String, body: String, sentTime: Long, hideInRealm: Boolean = true, asRead: Boolean = false, errorCode: Int, canUseSubId: Boolean = true, subId: Long = 0L) {
        if(!hideInRealm) {
            addMessageToRealmInboxAsRead(context, address, body, sentTime, asRead, errorCode, canUseSubId, subId)
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

    private fun addMessageToRealmInboxAsRead(context: Context, address: String, body: String, sentTime: Long, asRead: Boolean = false, errorCode: Int = 0, canUseSubId: Boolean = true, subId: Long = 0L) {
        val realmInstance = Realm.getDefaultInstance()

        var managedMessage: Message? = null

        var maxValue: Long = realmInstance.use { realm ->
            realm.where(Message::class.java).max("id")?.toLong() ?: 0L
        }

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

        realmInstance.executeTransaction { managedMessage = realmInstance.copyToRealmOrUpdate(message) }

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

        if(asRead) {
            values.put(Telephony.Sms.READ, 1)
        } else {
            values.put(Telephony.Sms.READ, 0)
        }

        val uri = context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)

        uri?.lastPathSegment?.toLong()?.let { id ->
            // Update the contentId after the message has been inserted to the content provider
            realmInstance.executeTransaction { managedMessage?.contentId = id }
        }

        //Update Conversation/Thread
        val conversation = realmInstance
            .where(Conversation::class.java)
            .equalTo("id", message.threadId)
            .findFirst()

        val lastMessage = realmInstance
            .where(Message::class.java)
            .equalTo("threadId", message.threadId)
            .sort("date", Sort.DESCENDING)
            .findFirst()

        if(conversation == null) {
            //syncRepository.syncMessage(uri!!)
            return
        } else {
            realmInstance.executeTransaction {
                conversation.lastMessage = lastMessage
            }
        }

        realmInstance.close()
    }
}