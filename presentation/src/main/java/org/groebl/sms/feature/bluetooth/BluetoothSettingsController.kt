package org.groebl.sms.feature.bluetooth

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.view.View
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bluelinelabs.conductor.RouterTransaction
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.Observable
import kotlinx.android.synthetic.main.bluetooth_settings_controller.*
import kotlinx.android.synthetic.main.settings_switch_widget.view.*
import org.groebl.sms.R
import org.groebl.sms.common.Navigator
import org.groebl.sms.common.QkChangeHandler
import org.groebl.sms.common.base.QkController
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.extensions.animateLayoutChanges
import org.groebl.sms.common.util.extensions.isInstalled
import org.groebl.sms.common.util.extensions.setVisible
import org.groebl.sms.common.widget.PreferenceView
import org.groebl.sms.feature.bluetooth.app.BluetoothAppActivity
import org.groebl.sms.feature.bluetooth.common.BluetoothDatabase
import org.groebl.sms.feature.bluetooth.common.BluetoothHelper
import org.groebl.sms.feature.bluetooth.common.BluetoothMessengerBlocked
import org.groebl.sms.feature.bluetooth.device.BluetoothDeviceActivity
import org.groebl.sms.feature.bluetooth.donate.BluetoothDonateActivity
import org.groebl.sms.feature.settings.about.AboutController
import org.groebl.sms.injection.appComponent
import org.groebl.sms.util.Preferences
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class BluetoothSettingsController : QkController<BluetoothSettingsView, BluetoothSettingsState, BluetoothSettingsPresenter>(), BluetoothSettingsView {

    @Inject lateinit var context: Context
    @Inject lateinit var colors: Colors

    @Inject lateinit var navigator: Navigator

    @Inject lateinit var prefs: Preferences

    @Inject override lateinit var presenter: BluetoothSettingsPresenter

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
        layoutRes = R.layout.bluetooth_settings_controller

        colors.themeObservable()
                .autoDisposable(scope())
                .subscribe { activity?.recreate() }
    }

    override fun onViewCreated() {
        //bluetooth_menu_main.postDelayed({ bluetooth_menu_main?.animateLayoutChanges = true }, 100)
        bluetooth_menu_full.postDelayed({ bluetooth_menu_full?.animateLayoutChanges = true }, 100)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.title_settings_bluetooth)
        showBackButton(true)
    }

    override fun preferenceFullClicks(): Observable<PreferenceView> = (0 until bluetooth_menu_full.childCount)
            .map { index -> bluetooth_menu_full.getChildAt(index) }
            .mapNotNull { view -> view as? PreferenceView }
            .map { bt_full_preference -> bt_full_preference.clicks().map { bt_full_preference } }
            .let { bluetooth_full_preferences -> Observable.merge(bluetooth_full_preferences) }

    override fun preferenceMainClicks(): Observable<PreferenceView> = (0 until bluetooth_menu_main.childCount)
            .map { index -> bluetooth_menu_main.getChildAt(index) }
            .mapNotNull { view -> view as? PreferenceView }
            .map { bt_main_preference -> bt_main_preference.clicks().map { bt_main_preference } }
            .let { bluetooth_menu_main -> Observable.merge(bluetooth_menu_main) }

    override fun render(state: BluetoothSettingsState) {
        var localBluetoothEnabled = state.bluetooth_enabled
        var localBluetoothTethering = state.bluetooth_tethering
        var localBluetoothOnlyOnConnect = state.bluetooth_only_on_connect
        var localBluetoothWhatsappToContact = state.bluetooth_whatsapp_to_contact
        var localBluetoothTelegramToContact = state.bluetooth_telegram_to_contact
        var localBluetoothSignalToContact = state.bluetooth_signal_to_contact

        val localBluetoothWhatsappInstalled = context.isInstalled("com.whatsapp")
        val localBluetoothTelegramInstalled = context.isInstalled("org.telegram.messenger")
        val localBluetoothSignalInstalled = context.isInstalled("org.thoughtcrime.securesms")

        if(!localBluetoothEnabled) {
            Thread { BluetoothHelper.deleteBluetoothMessages(context, false) }.start()
            Thread { BluetoothDatabase.deleteBluetoothDbData(context, false) }.start()
        }

        //Forwarding enabled but not default SMS-App or has no Notification-Access
        if(localBluetoothEnabled and (!BluetoothHelper.isDefaultSms(context) or !BluetoothHelper.hasNotificationAccess(context))) {
            prefs.bluetooth_enabled.set(false)
            localBluetoothEnabled = false

            //if(!BluetoothHelper.isDefaultSms(context))          { requestDefaultSms() }
            //if(!BluetoothHelper.hasNotificationAccess(context)) { showNotificationAccess() }
        }

        //WhatsApp-to-Contact enabled but no Contact-Permission
        if(localBluetoothEnabled && (state.bluetooth_whatsapp_to_contact || state.bluetooth_signal_to_contact || state.bluetooth_telegram_to_contact) && !BluetoothHelper.hasContactPermission(context)) {
            prefs.bluetooth_whatsapp_to_contact.set(false)
            prefs.bluetooth_signal_to_contact.set(false)
            prefs.bluetooth_telegram_to_contact.set(false)
            localBluetoothWhatsappToContact = false
            localBluetoothSignalToContact = false
            localBluetoothTelegramToContact = false
            BluetoothHelper.requestContactPermission(activity!!)
        }

        //Check if permission.BLUETOOTH_CONNECT is set
        if(localBluetoothEnabled && localBluetoothOnlyOnConnect && !BluetoothHelper.hasBluetoothPermission(context)) {
            prefs.bluetooth_only_on_connect.set(false)
            localBluetoothOnlyOnConnect = false
            BluetoothHelper.requestBluetoothPermission(activity!!)
        }

/*
        //Tethering enabled but no system-write permission
        if(localBluetoothEnabled && state.bluetooth_tethering && Build.VERSION.SDK_INT >= 23) {
            if(!Settings.System.canWrite(context)) {
                prefs.bluetooth_tethering.set(false)
                localBluetoothTethering = false

                val intentWriteSettings = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intentWriteSettings.data = Uri.parse("package:" + context.packageName)
                intentWriteSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                AlertDialog.Builder(activity!!)
                        .setTitle(R.string.main_permission_required)
                        .setMessage(String.format(context.getString(R.string.settings_bluetooth_tethering_dialog), context.getString(R.string.app_name)))
                        .setPositiveButton(R.string.title_settings) { _, _ -> startActivity(Intent(intentWriteSettings)) }
                        .setNegativeButton(R.string.button_cancel, null)
                        .show()
            }
        }
*/

        bluetooth_menu_main.setVisible(localBluetoothEnabled)
        bluetooth_select_device.setVisible(localBluetoothOnlyOnConnect)
        bluetooth_autodelete.setVisible(localBluetoothOnlyOnConnect)

        bluetooth_delayed_read.setVisible(state.bluetooth_save_read)

        bluetooth_whatsapp_divider.setVisible(localBluetoothWhatsappInstalled)
        bluetooth_whatsapp_category.setVisible(localBluetoothWhatsappInstalled)
        bluetooth_whatsapp_to_contact.setVisible(localBluetoothWhatsappInstalled)
        bluetooth_whatsapp_blocked_group.setVisible(localBluetoothWhatsappToContact && localBluetoothWhatsappInstalled)
        bluetooth_whatsapp_blocked_contact.setVisible(localBluetoothWhatsappToContact && localBluetoothWhatsappInstalled)
        bluetooth_whatsapp_hide_prefix.setVisible(localBluetoothWhatsappToContact && localBluetoothWhatsappInstalled)

        bluetooth_signal_divider.setVisible(localBluetoothSignalInstalled)
        bluetooth_signal_category.setVisible(localBluetoothSignalInstalled)
        bluetooth_signal_to_contact.setVisible(localBluetoothSignalInstalled)
        bluetooth_signal_blocked_group.setVisible(localBluetoothSignalToContact && localBluetoothSignalInstalled)
        bluetooth_signal_blocked_contact.setVisible(localBluetoothSignalToContact && localBluetoothSignalInstalled)
        bluetooth_signal_hide_prefix.setVisible(localBluetoothSignalToContact && localBluetoothSignalInstalled)

        bluetooth_telegram_divider.setVisible(localBluetoothTelegramInstalled)
        bluetooth_telegram_category.setVisible(localBluetoothTelegramInstalled)
        bluetooth_telegram_to_contact.setVisible(localBluetoothTelegramInstalled)
        bluetooth_telegram_blocked_group.setVisible(localBluetoothTelegramToContact && localBluetoothTelegramInstalled)
        bluetooth_telegram_blocked_contact.setVisible(localBluetoothTelegramToContact && localBluetoothTelegramInstalled)
        bluetooth_telegram_hide_prefix.setVisible(localBluetoothTelegramToContact && localBluetoothTelegramInstalled)

        bluetooth_more_divider.setVisible(localBluetoothOnlyOnConnect)
        bluetooth_more_cat.setVisible(localBluetoothOnlyOnConnect)
        bluetooth_max_vol.setVisible(localBluetoothOnlyOnConnect)
        bluetooth_tethering.setVisible(localBluetoothOnlyOnConnect)

        bluetooth_battery.setVisible(state.bluetooth_enabled)

        bluetooth_enabled.checkbox.isChecked = localBluetoothEnabled
        bluetooth_only_on_connect.checkbox.isChecked = localBluetoothOnlyOnConnect
        bluetooth_autodelete.checkbox.isChecked = state.bluetooth_autodelete
        bluetooth_save_read.checkbox.isChecked = state.bluetooth_save_read
        bluetooth_delayed_read.checkbox.isChecked = state.bluetooth_delayed_read
        bluetooth_emoji.checkbox.isChecked = state.bluetooth_emoji
        bluetooth_appname_as_sender_text.checkbox.isChecked = state.bluetooth_appname_as_sender_text
        bluetooth_appname_as_sender_number.checkbox.isChecked = state.bluetooth_appname_as_sender_number
        bluetooth_whatsapp_to_contact.checkbox.isChecked = localBluetoothWhatsappToContact
        bluetooth_signal_to_contact.checkbox.isChecked = localBluetoothSignalToContact
        bluetooth_telegram_to_contact.checkbox.isChecked = localBluetoothTelegramToContact
        bluetooth_whatsapp_hide_prefix.checkbox.isChecked = state.bluetooth_whatsapp_hide_prefix
        bluetooth_signal_hide_prefix.checkbox.isChecked = state.bluetooth_signal_hide_prefix
        bluetooth_telegram_hide_prefix.checkbox.isChecked = state.bluetooth_telegram_hide_prefix
        bluetooth_max_vol.checkbox.isChecked = state.bluetooth_max_vol
        bluetooth_tethering.checkbox.isChecked = localBluetoothTethering

        //Connected and Last-Connected-Device and Time available
        if (prefs.bluetooth_current_status.get() &&
            prefs.bluetooth_last_connect.get() > 0L &&
            prefs.bluetooth_last_connect_device.get() != "" &&
            localBluetoothOnlyOnConnect)
        {
            bluetooth_device_status.setVisible(true)
            bluetooth_device_status.title = String.format(context.getString(R.string.settings_bluetooth_device_status_connected), prefs.bluetooth_last_connect_device.get(), SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(prefs.bluetooth_last_connect.get()))
        }
        //Not Connected and Last-Connected-Device and Time available
        else if (!prefs.bluetooth_current_status.get() &&
                    prefs.bluetooth_last_disconnect.get() > 0L &&
                    prefs.bluetooth_last_connect_device.get() != "" &&
                    localBluetoothOnlyOnConnect)
        {
            bluetooth_device_status.setVisible(true)
            bluetooth_device_status.title = String.format(context.getString(R.string.settings_bluetooth_device_status_disconnected), prefs.bluetooth_last_connect_device.get(), SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(prefs.bluetooth_last_disconnect.get()))

        }
        else
        //Hide the Menu-Entry
        { bluetooth_device_status.setVisible(false) }

    }

    override fun showBluetoothAbout() {
        router.pushController(RouterTransaction.with(AboutController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

    override fun showBluetoothBatteryOptimize() {
        AlertDialog.Builder(activity!!)
                .setTitle(R.string.settings_bluetooth_battery_title)
                .setMessage(String.format(context.getString(R.string.settings_bluetooth_battery_dialog), context.getString(R.string.app_name)))
                .setPositiveButton(R.string.button_info) { _, _ -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BluetoothHelper.getDontKillMyAppUrl("?app=SMS%20%26%20Notifications")))) }
                .setNegativeButton(R.string.button_cancel, null)
                .show()
    }

    override fun showNotificationAccess() {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent)
    }

    override fun requestDefaultSms() {
        navigator.showDefaultSmsDialog(activity!!)
    }

    override fun showBluetoothDonate() {
        val intent = Intent(context, BluetoothDonateActivity::class.java)
        startActivity(intent)
    }

    override fun showBluetoothApps() {
        val intent = Intent(context, BluetoothAppActivity::class.java)
        startActivity(intent)
    }

    override fun showBluetoothDevices() {
        val intent = Intent(context, BluetoothDeviceActivity::class.java)
        startActivity(intent)
    }

    override fun showBluetoothBlockedContactWhatsApp() {

        val arrlist = ArrayList<String>()
        for(item in prefs.bluetooth_whatsapp_blocked_contact.get()) {
            val name = BluetoothHelper.findWhatsAppNameFromNumber(context, item)
            if(name == "")   { arrlist.add(item) }
            else             { arrlist.add("$item\n($name)") }
        }
        val array = arrayOfNulls<String>(arrlist.size)
        arrlist.toArray(array)

        val editText = EditText(activity!!)
        editText.hint = "+49 998 877 665"

        AlertDialog.Builder(activity!!)
                .setTitle(R.string.settings_bluetooth_block_whatsapp_contact_title)
                .setItems(array) { _, which ->
                    val parts = array[which]!!.split("\n")
                    AlertDialog.Builder(activity!!)
                            .setTitle(R.string.settings_bluetooth_block_unblock_confirm)
                            .setCancelable(false)
                            .setMessage(context.getString(R.string.settings_bluetooth_block_unblock_summary, parts[0]))
                            .setPositiveButton(R.string.button_yes) { _, _ ->
                                BluetoothMessengerBlocked.setMessengerUnblock(context, parts[0], false, "WhatsApp")
                                //Toast.makeText(context, "Unblock: " + array[which], Toast.LENGTH_LONG).show()
                            }
                            .setNegativeButton(R.string.button_no) { dialog, _ -> dialog.cancel() }
                            .show()
                }
                .setPositiveButton(R.string.button_add) { _, _ ->
                    AlertDialog.Builder(activity!!)
                        .setTitle(context.getString(R.string.settings_bluetooth_block_contact_add_title, "WhatsApp"))
                        .setMessage(R.string.settings_bluetooth_block_contact_ask_summary)
                        .setCancelable(false)
                        .setPositiveButton(R.string.settings_bluetooth_block_phonebook) { _, _ -> selectContact() }
                        .setNeutralButton(R.string.settings_bluetooth_block_manual) { _, _ ->
                            AlertDialog.Builder(activity!!)
                                .setTitle(context.getString(R.string.settings_bluetooth_block_contact_add_title, "WhatsApp"))
                                .setCancelable(false)
                                .setMessage(R.string.settings_bluetooth_block_contact_nr_add_summary)
                                .setView(editText)
                                .setPositiveButton(R.string.button_add) { _, _ ->
                                    if (editText.text.isNotEmpty()) {
                                        BluetoothMessengerBlocked.setMessengerBlock(context, PhoneNumberUtils.stripSeparators(editText.text.toString()), false, "WhatsApp")
                                        //Toast.makeText(context, "Add " + PhoneNumberUtils.stripSeparators(editText.text.toString()), Toast.LENGTH_LONG).show()
                                    }
                                }
                                .setNegativeButton(R.string.button_cancel) { dialog, _ -> dialog.cancel() }
                                .show()
                        }
                        .show()

                }
                .setNegativeButton(R.string.button_cancel) { dialog, _ -> dialog.cancel() }
                .show()
    }

    override fun showBluetoothBlockedContactByName(MessengerType: String) {
        val items = when (MessengerType) {
            "Signal" ->     { prefs.bluetooth_signal_blocked_contact.get() }
            "Telegram" ->   { prefs.bluetooth_telegram_blocked_contact.get() }
            else ->         { return; }
        }

        val array = arrayOfNulls<String>(items.size)
        items.toHashSet().toArray(array)

        val editText = EditText(activity!!)

        AlertDialog.Builder(activity!!)
                .setTitle(context.getString(R.string.settings_bluetooth_block_contact_add_title, MessengerType))
                .setItems(array) { _, which ->
                    AlertDialog.Builder(activity!!)
                            .setTitle(R.string.settings_bluetooth_block_unblock_confirm)
                            .setCancelable(false)
                            .setMessage(context.getString(R.string.settings_bluetooth_block_unblock_summary, array[which]))
                            .setPositiveButton(R.string.button_yes) { _, _ ->
                                BluetoothMessengerBlocked.setMessengerUnblock(context, array[which], false, MessengerType)
                                //Toast.makeText(context, "Unblock: " + array[which], Toast.LENGTH_LONG).show()
                            }
                            .setNegativeButton(R.string.button_no) { dialog, _ -> dialog.cancel() }
                            .show()
                }
                .setPositiveButton(R.string.button_add) { _, _ ->
                    AlertDialog.Builder(activity!!)
                            .setTitle(context.getString(R.string.settings_bluetooth_block_contact_add_title, MessengerType))
                            .setCancelable(false)
                            .setMessage(R.string.settings_bluetooth_block_contact_name_add_summary)
                            .setView(editText)
                            .setPositiveButton(R.string.button_add) { _, _ ->
                                if(editText.text.isNotEmpty()) {
                                    BluetoothMessengerBlocked.setMessengerBlock(context, editText.text.toString().trim(), false, MessengerType)
                                    //Toast.makeText(context, "Add: " + editText.text.toString(), Toast.LENGTH_LONG).show()
                                }
                            }
                            .setNegativeButton(R.string.button_cancel) { dialog, _ -> dialog.cancel() }
                            .show()
                }
                .setNegativeButton(R.string.button_cancel) { dialog, _ -> dialog.cancel() }
                .show()
    }

    override fun showBluetoothBlockedGroup(MessengerType: String) {
        val items = when (MessengerType) {
            "WhatsApp" ->   { prefs.bluetooth_whatsapp_blocked_group.get() }
            "Signal" ->     { prefs.bluetooth_signal_blocked_group.get() }
            "Telegram" ->   { prefs.bluetooth_telegram_blocked_group.get() }
            else ->         { return; }
        }

        val array = arrayOfNulls<String>(items.size)
        items.toHashSet().toArray(array)

        val editText = EditText(activity!!)

        AlertDialog.Builder(activity!!)
                .setTitle(context.getString(R.string.settings_bluetooth_block_group_title, MessengerType))
                .setItems(array) { _, which ->
                    AlertDialog.Builder(activity!!)
                            .setTitle(R.string.settings_bluetooth_block_unblock_confirm)
                            .setCancelable(false)
                            .setMessage(context.getString(R.string.settings_bluetooth_block_unblock_summary, array[which]))
                            .setPositiveButton(R.string.button_yes) { _, _ ->
                                BluetoothMessengerBlocked.setMessengerUnblock(context, array[which], true, MessengerType)
                                //Toast.makeText(context, "Unblock: " + array[which], Toast.LENGTH_LONG).show()
                            }
                            .setNegativeButton(R.string.button_no) { dialog, _ -> dialog.cancel() }
                            .show()
                }
                .setPositiveButton(R.string.button_add) { _, _ ->
                    AlertDialog.Builder(activity!!)
                            .setTitle(context.getString(R.string.settings_bluetooth_block_group_add_title, MessengerType))
                            .setCancelable(false)
                            .setMessage(R.string.settings_bluetooth_block_group_add_summary)
                            .setView(editText)
                            .setPositiveButton(R.string.button_add) { _, _ ->
                                if(editText.text.isNotEmpty()) {
                                    BluetoothMessengerBlocked.setMessengerBlock(context, editText.text.toString().trim(), true, MessengerType)
                                    //Toast.makeText(context, "Add: " + editText.text.toString(), Toast.LENGTH_LONG).show()
                                }
                            }
                            .setNegativeButton(R.string.button_cancel) { dialog, _ -> dialog.cancel() }
                            .show()
                }
                .setNegativeButton(R.string.button_cancel) { dialog, _ -> dialog.cancel() }
                .show()
    }

    private fun selectContact() {
        when {
            Build.VERSION.SDK_INT < 23 -> startActivityForResult(Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), 2)
            ContextCompat.checkSelfPermission(activity!!, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED -> ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.READ_CONTACTS), 0)
            else -> startActivityForResult(Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), 2)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            try {
                val uri = intentData!!.data
                val cursor = context.contentResolver.query(uri!!, null, null, null, null)

                cursor?.use {
                    if (cursor.moveToFirst()) {
                        val cursorWA = context.contentResolver.query(ContactsContract.RawContacts.CONTENT_URI, null, ContactsContract.RawContacts.CONTACT_ID + " = ? AND " + ContactsContract.RawContacts.ACCOUNT_TYPE + " = ?", arrayOf(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)), "com.whatsapp"), null)
                        cursorWA?.use {
                            if (cursorWA.moveToFirst()) {
                                val cIndex = cursorWA.getColumnIndex(ContactsContract.RawContacts.SYNC1)
                                val split = cursorWA.getString(cIndex).split("@")
                                BluetoothMessengerBlocked.setMessengerBlock(context, "+" + split[0], false, "WhatsApp")
                            } else {
                                AlertDialog.Builder(activity!!)
                                        .setTitle(R.string.settings_bluetooth_block_whatsapp_contact_add_title)
                                        .setMessage(R.string.settings_bluetooth_block_whatsapp_contact_no_wa)
                                        .setCancelable(true)
                                        .setPositiveButton(R.string.bluetooth_alert_button_ok) { dialog, _ -> dialog.cancel() }
                                        .show()
                            }
                        }
                    } else {
                        AlertDialog.Builder(activity!!)
                                .setTitle(R.string.settings_bluetooth_block_whatsapp_contact_add_title)
                                .setMessage(context.getString(R.string.compose_details_error_code, 2))
                                .setCancelable(true)
                                .setPositiveButton(R.string.bluetooth_alert_button_ok) { dialog, _ -> dialog.cancel() }
                                .show()
                    }
                }
            }
            catch (e: Exception) {
                AlertDialog.Builder(activity!!)
                        .setTitle(R.string.settings_bluetooth_block_whatsapp_contact_add_title)
                        .setMessage(context.getString(R.string.compose_details_error_code, 1))
                        .setCancelable(true)
                        .setPositiveButton(R.string.bluetooth_alert_button_ok) { dialog, _ -> dialog.cancel() }
                        .show()
            }
        }
    }

}