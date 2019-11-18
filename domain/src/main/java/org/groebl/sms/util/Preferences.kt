/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.groebl.sms.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import org.groebl.sms.common.util.extensions.versionCode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Preferences @Inject constructor(context: Context, private val rxPrefs: RxSharedPreferences) {

    companion object {
        const val NIGHT_MODE_SYSTEM = 0
        const val NIGHT_MODE_OFF = 1
        const val NIGHT_MODE_ON = 2
        const val NIGHT_MODE_AUTO = 3

        const val TEXT_SIZE_SMALL = 0
        const val TEXT_SIZE_NORMAL = 1
        const val TEXT_SIZE_LARGE = 2
        const val TEXT_SIZE_LARGER = 3

        const val NOTIFICATION_PREVIEWS_ALL = 0
        const val NOTIFICATION_PREVIEWS_NAME = 1
        const val NOTIFICATION_PREVIEWS_NONE = 2

        const val NOTIFICATION_ACTION_NONE = 0
        const val NOTIFICATION_ACTION_READ = 1
        const val NOTIFICATION_ACTION_REPLY = 2
        const val NOTIFICATION_ACTION_CALL = 3
        const val NOTIFICATION_ACTION_DELETE = 4

        const val SEND_DELAY_NONE = 0
        const val SEND_DELAY_SHORT = 1
        const val SEND_DELAY_MEDIUM = 2
        const val SEND_DELAY_LONG = 3

        const val SWIPE_ACTION_NONE = 0
        const val SWIPE_ACTION_ARCHIVE = 1
        const val SWIPE_ACTION_DELETE = 2
        const val SWIPE_ACTION_CALL = 3
        const val SWIPE_ACTION_READ = 4
        const val SWIPE_ACTION_UNREAD = 5

        const val BLOCKING_MANAGER_QKSMS = 0
        const val BLOCKING_MANAGER_CC = 1
        const val BLOCKING_MANAGER_SIA = 2
    }

    // Internal
    val night = rxPrefs.getBoolean("night", false)
    val canUseSubId = rxPrefs.getBoolean("canUseSubId", true)
    val version = rxPrefs.getInteger("version", context.versionCode)
    val changelogVersion = rxPrefs.getInteger("changelogVersion", context.versionCode)
    @Deprecated("This should only be accessed when migrating to @blockingManager")
    val sia = rxPrefs.getBoolean("sia", false)

    // User configurable
    val nightMode = rxPrefs.getInteger("nightMode", when (Build.VERSION.SDK_INT >= 29) {
        true -> NIGHT_MODE_SYSTEM
        false -> NIGHT_MODE_OFF
    })
    val nightStart = rxPrefs.getString("nightStart", "18:00")
    val nightEnd = rxPrefs.getString("nightEnd", "6:00")
    val black = rxPrefs.getBoolean("black", false)
    val systemFont = rxPrefs.getBoolean("systemFont", false)
    val textSize = rxPrefs.getInteger("textSize", TEXT_SIZE_NORMAL)
    val blockingManager = rxPrefs.getInteger("blockingManager", BLOCKING_MANAGER_QKSMS)
    val drop = rxPrefs.getBoolean("drop", false)
    val notifAction1 = rxPrefs.getInteger("notifAction1", NOTIFICATION_ACTION_READ)
    val notifAction2 = rxPrefs.getInteger("notifAction2", NOTIFICATION_ACTION_REPLY)
    val notifAction3 = rxPrefs.getInteger("notifAction3", NOTIFICATION_ACTION_NONE)
    val qkreply = rxPrefs.getBoolean("qkreply", Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
    val qkreplyTapDismiss = rxPrefs.getBoolean("qkreplyTapDismiss", true)
    val sendDelay = rxPrefs.getInteger("sendDelay", SEND_DELAY_NONE)
    val swipeRight = rxPrefs.getInteger("swipeRight", SWIPE_ACTION_ARCHIVE)
    val swipeLeft = rxPrefs.getInteger("swipeLeft", SWIPE_ACTION_ARCHIVE)
    val autoEmoji = rxPrefs.getBoolean("autoEmoji", true)
    val delivery = rxPrefs.getBoolean("delivery", false)
    val signature = rxPrefs.getString("signature", "")
    val unicode = rxPrefs.getBoolean("unicode", false)
    val mobileOnly = rxPrefs.getBoolean("mobileOnly", false)
    val mmsSize = rxPrefs.getInteger("mmsSize", 300)
    val logging = rxPrefs.getBoolean("logging", false)

    val bluetooth_enabled = rxPrefs.getBoolean("bluetoothEnabled", false)
    val bluetooth_apps = rxPrefs.getStringSet("bluetoothApps", HashSet<String>())
    val bluetooth_devices = rxPrefs.getStringSet("bluetoothDevices", HashSet<String>())
    val bluetooth_only_on_connect = rxPrefs.getBoolean("bluetoothOnlyOnConnect", true)
    val bluetooth_autodelete = rxPrefs.getBoolean("bluetoothAutodelete", true)
    val bluetooth_save_read = rxPrefs.getBoolean("bluetoothSaveRead", false)
    val bluetooth_delayed_read = rxPrefs.getBoolean("bluetoothDelayedRead", false)
    val bluetooth_emoji = rxPrefs.getBoolean("bluetoothEmoji", true)
    val bluetooth_appname_as_sender_text = rxPrefs.getBoolean("bluetoothAppnameAsText", false)
    val bluetooth_appname_as_sender_number = rxPrefs.getBoolean("bluetoothAppnameToNumber", false)
    val bluetooth_whatsapp_to_contact = rxPrefs.getBoolean("bluetoothWhatsAppToContact", true)
    val bluetooth_whatsapp_hide_prefix = rxPrefs.getBoolean("bluetoothWhatsAppHidePrefix", true)
    val bluetooth_max_vol = rxPrefs.getBoolean("bluetoothMaxVol", false)
    val bluetooth_tethering = rxPrefs.getBoolean("bluetoothTethering", false)
    val bluetooth_whatsapp_blocked_group = rxPrefs.getStringSet("bluetoothWhatsAppBlockedGroup", HashSet<String>())
    val bluetooth_whatsapp_blocked_contact = rxPrefs.getStringSet("bluetoothWhatsAppBlockedContact", HashSet<String>())
    val bluetooth_current_status = rxPrefs.getBoolean("bluetoothCurrentStatus", false)
    val bluetooth_last_connect = rxPrefs.getLong("bluetoothLastConnect", 0L)
    val bluetooth_last_disconnect = rxPrefs.getLong("bluetoothLastDisconnect", 0L)
    val bluetooth_last_connect_device = rxPrefs.getString("bluetoothLastDevice", "")
    val bluetooth_sync_dismiss = rxPrefs.getBoolean("bluetoothAlertSyncDismiss", false)

    init {
        // Migrate from old night mode preference to new one, now that we support android Q night mode
        val nightModeSummary = rxPrefs.getInteger("nightModeSummary")
        if (nightModeSummary.isSet) {
            nightMode.set(when (nightModeSummary.get()) {
                0 -> NIGHT_MODE_OFF
                1 -> NIGHT_MODE_ON
                2 -> NIGHT_MODE_AUTO
                else -> NIGHT_MODE_OFF
            })
            nightModeSummary.delete()
        }
    }

    fun theme(threadId: Long = 0): Preference<Int> {
        val default = rxPrefs.getInteger("theme", 0xFF0097A7.toInt())

        return when (threadId) {
            0L -> default
            else -> rxPrefs.getInteger("theme_$threadId", default.get())
        }
    }

    fun notifications(threadId: Long = 0): Preference<Boolean> {
        val default = rxPrefs.getBoolean("notifications", true)

        return when (threadId) {
            0L -> default
            else -> rxPrefs.getBoolean("notifications_$threadId", default.get())
        }
    }

    fun notificationPreviews(threadId: Long = 0): Preference<Int> {
        val default = rxPrefs.getInteger("notification_previews", 0)

        return when (threadId) {
            0L -> default
            else -> rxPrefs.getInteger("notification_previews_$threadId", default.get())
        }
    }

    fun vibration(threadId: Long = 0): Preference<Boolean> {
        val default = rxPrefs.getBoolean("vibration", true)

        return when (threadId) {
            0L -> default
            else -> rxPrefs.getBoolean("vibration$threadId", default.get())
        }
    }

    fun ringtone(threadId: Long = 0): Preference<String> {
        val default = rxPrefs.getString("ringtone", Settings.System.DEFAULT_NOTIFICATION_URI.toString())

        return when (threadId) {
            0L -> default
            else -> rxPrefs.getString("ringtone_$threadId", default.get())
        }
    }
}