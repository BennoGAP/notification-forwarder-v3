package org.groebl.sms.common.util

import android.content.Context
import android.provider.ContactsContract
import timber.log.Timber

class BluetoothPhoneNumberHelper {

    fun findNumberFromWhatsAppName(context: Context, NrDisplayName: String): String {
        var setNumber = ""
        try {
            val c = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DATA1),
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " = ? AND " + ContactsContract.RawContacts.ACCOUNT_TYPE + " = ?",
                arrayOf(NrDisplayName, "com.whatsapp"), null
            )

            c.use { c ->
                if ((c != null) && c.moveToFirst()) {
                    setNumber = c.getString(0)
                }
            }
        } catch (e: Exception) {
            Timber.w(e)
        }

        return setNumber.trim()
    }

    fun findNumberFromSignalName(context: Context, NrDisplayName: String): String {
        var setNumber = ""
        try {
            val c = context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                null,
                ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY + " = ? AND " + ContactsContract.RawContacts.ACCOUNT_TYPE + " = ?",
                arrayOf(NrDisplayName, "org.thoughtcrime.securesms"), null
            )

            c.use { c ->
                if ((c != null) && c.moveToFirst()) {
                    val cIndex = c.getColumnIndex(ContactsContract.RawContacts.SYNC1)
                    if (cIndex > 0) {
                        setNumber = c.getString(cIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e)
        }

        return setNumber.trim()
    }

    fun findNumberFromTelegramName(context: Context, NrDisplayName: String): String {
        var setNumber = ""
        try {
            val c = context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                null,
                ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY + " = ? AND " + ContactsContract.RawContacts.ACCOUNT_TYPE + " = ?",
                arrayOf(NrDisplayName, "org.telegram.messenger"), null
            )

            c.use { c ->
                if ((c != null) && c.moveToFirst()) {
                    val cIndex = c.getColumnIndex(ContactsContract.RawContacts.SYNC1)
                    if (cIndex > 0) {
                        setNumber = c.getString(cIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e)
        }

        return setNumber.trim()
    }
}