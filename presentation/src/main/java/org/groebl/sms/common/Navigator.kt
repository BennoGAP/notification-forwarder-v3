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
package org.groebl.sms.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import com.klinker.android.send_message.Utils
import org.groebl.sms.BuildConfig
import org.groebl.sms.common.util.BillingManager
import org.groebl.sms.feature.backup.BackupActivity
import org.groebl.sms.feature.blocked.BlockedActivity
import org.groebl.sms.feature.bluetooth.BluetoothSettingsActivity
import org.groebl.sms.feature.bluetooth.common.BluetoothHelper
import org.groebl.sms.feature.bluetooth.donate.BluetoothDonateActivity
import org.groebl.sms.feature.compose.ComposeActivity
import org.groebl.sms.feature.conversationinfo.ConversationInfoActivity
import org.groebl.sms.feature.gallery.GalleryActivity
import org.groebl.sms.feature.notificationprefs.NotificationPrefsActivity
import org.groebl.sms.feature.scheduled.ScheduledActivity
import org.groebl.sms.feature.settings.SettingsActivity
import org.groebl.sms.manager.AnalyticsManager
import org.groebl.sms.manager.NotificationManager
import org.groebl.sms.manager.PermissionManager
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class Navigator @Inject constructor(
        private val context: Context,
        private val analyticsManager: AnalyticsManager,
        private val billingManager: BillingManager,
        private val notificationManager: NotificationManager,
        private val permissions: PermissionManager
) {

    private fun startActivity(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun startActivityExternal(intent: Intent) {
        if (intent.resolveActivity(context.packageManager) != null) {
            startActivity(intent)
        } else {
            startActivity(Intent.createChooser(intent, null))
        }
    }

    fun showBluetoothAccess() {
        if(!Utils.isDefaultSmsApp(context)) { showDefaultSmsDialog() }
        if(!BluetoothHelper.hasNotificationAccess(context)) { showNotificationAccess() }
    }

    fun showNotificationAccess() {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        startActivity(intent)
    }

    fun showDefaultSmsDialog() {
        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
        if (Telephony.Sms.getDefaultSmsPackage(context) != context.packageName) {
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
        }
        startActivity(intent)
    }

    fun showCompose(body: String? = null, images: List<Uri>? = null) {
        val intent = Intent(context, ComposeActivity::class.java)
        intent.putExtra(Intent.EXTRA_TEXT, body)

        images?.takeIf { it.isNotEmpty() }?.let {
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(images))
        }

        startActivity(intent)
    }

    fun showConversation(threadId: Long, query: String? = null) {
        val intent = Intent(context, ComposeActivity::class.java)
                .putExtra("threadId", threadId)
                .putExtra("query", query)
        startActivity(intent)
    }

    fun showConversationInfo(threadId: Long) {
        val intent = Intent(context, ConversationInfoActivity::class.java)
        intent.putExtra("threadId", threadId)
        startActivity(intent)
    }

    fun showMedia(partId: Long) {
        val intent = Intent(context, GalleryActivity::class.java)
        intent.putExtra("partId", partId)
        startActivity(intent)
    }

    fun showBackup() {
        analyticsManager.track("Viewed Backup")
        startActivity(Intent(context, BackupActivity::class.java))
    }

    fun showScheduled() {
        analyticsManager.track("Viewed Scheduled")
        val intent = Intent(context, ScheduledActivity::class.java)
        startActivity(intent)
    }

    fun showSettings() {
        val intent = Intent(context, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun showBluetoothSettings() {
        analyticsManager.track("Viewed BluetoothSettings")
        val intent = Intent(context, BluetoothSettingsActivity::class.java)
        startActivity(intent)
    }

    fun showBluetoothDonateScreen() {
        val intent = Intent(context, BluetoothDonateActivity::class.java)
        startActivity(intent)
    }

    fun showDonationBluetooth() {
        analyticsManager.track("Clicked Donate")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://android.groebl.org/sms/donate"))
        startActivity(intent)
    }

    fun showFAQ() {
        analyticsManager.track("Clicked FAQ")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://android.groebl.org/sms/faq/"))
        startActivity(intent)
    }

    fun showDeveloper() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/moezbhatti"))
        startActivity(intent)
    }

    fun showSourceCode() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/moezbhatti/qksms"))
        startActivity(intent)
    }

    fun showChangelog() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/moezbhatti/qksms/releases"))
        startActivity(intent)
    }

    fun showLicense() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/moezbhatti/qksms/blob/master/LICENSE"))
        startActivity(intent)
    }

    fun showBlockedConversations() {
        val intent = Intent(context, BlockedActivity::class.java)
        startActivity(intent)
    }

    fun makePhoneCall(address: String) {
        val action = if (permissions.hasCalling()) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val intent = Intent(action, Uri.parse("tel:$address"))
        startActivityExternal(intent)
    }

    fun showRating() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=org.groebl.sms"))
                .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                        or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                        or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=org.groebl.sms")))
        }
    }

    /**
     * Launch the Play Store and display the Should I Answer? listing
     */
    fun showSia() {
        val url = "https://play.google.com/store/apps/details?id=org.mistergroup.shouldianswerpersonal"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    fun showSupport() {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("android@groebl.org"))
        intent.putExtra(Intent.EXTRA_SUBJECT, "[Notification Forwarder Pro]")
        intent.putExtra(Intent.EXTRA_TEXT, StringBuilder("\n\n")
                .append("\n\n--- Please write your message above this line ---\n\n")
                .append("Package: ${context.packageName}\n")
                .append("Version: ${BuildConfig.VERSION_NAME}\n")
                .append("Device: ${Build.BRAND} ${Build.MODEL}\n")
                .append("SDK: ${Build.VERSION.SDK_INT}\n")
                .append("Donated".takeIf { billingManager.upgradeStatus.blockingFirst() } ?: "")
                .toString())
        startActivityExternal(intent)
    }

    fun showInvite() {
        analyticsManager.track("Clicked Invite")
        Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "https://android.groebl.org")
                .let { Intent.createChooser(it, null) }
                .let(this::startActivityExternal)
    }

    fun addContact(address: String) {
        val uri = Uri.parse("tel: $address")
        var intent = Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, uri)

        if (intent.resolveActivity(context.packageManager) == null) {
            intent = Intent(Intent.ACTION_INSERT)
                    .setType(ContactsContract.Contacts.CONTENT_TYPE)
                    .putExtra(ContactsContract.Intents.Insert.PHONE, address)
        }

        startActivityExternal(intent)
    }

    fun saveVcard(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "text/x-vcard")

        startActivityExternal(intent)
    }

    fun showNotificationSettings(threadId: Long = 0) {
        val intent = Intent(context, NotificationPrefsActivity::class.java)
        intent.putExtra("threadId", threadId)
        startActivity(intent)
    }

    fun showNotificationChannel(threadId: Long = 0) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (threadId != 0L) {
                notificationManager.createNotificationChannel(threadId)
            }

            val channelId = notificationManager.buildNotificationChannelId(threadId)
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            startActivity(intent)
        }
    }

}