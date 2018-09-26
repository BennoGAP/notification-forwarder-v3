package org.groebl.sms.feature.bluetooth

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import android.view.View
import android.widget.EditText
import com.bluelinelabs.conductor.RouterTransaction
import com.jakewharton.rxbinding2.view.clicks
import com.klinker.android.send_message.Utils
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Observable
import kotlinx.android.synthetic.main.bluetooth_settings_controller.*
import kotlinx.android.synthetic.main.settings_switch_widget.view.*
import org.groebl.sms.R
import org.groebl.sms.common.Navigator
import org.groebl.sms.common.QkChangeHandler
import org.groebl.sms.common.base.QkController
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.extensions.animateLayoutChanges
import org.groebl.sms.common.util.extensions.setVisible
import org.groebl.sms.common.widget.PreferenceView
import org.groebl.sms.feature.bluetooth.app.BluetoothAppActivity
import org.groebl.sms.feature.bluetooth.common.BluetoothBatteryUtils
import org.groebl.sms.feature.bluetooth.common.BluetoothHelper
import org.groebl.sms.feature.bluetooth.common.BluetoothWABlocked
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
        bluetooth_menu_main.postDelayed({ bluetooth_menu_main?.animateLayoutChanges = true }, 100)
        bluetooth_menu_full.postDelayed({ bluetooth_menu_full?.animateLayoutChanges = true }, 100)

        if (prefs.bluetooth_enabled.get()) {
            var info_msg = ""
            if (prefs.bluetooth_enabled.get() && BluetoothHelper.hasNotificationAccess(context) && !BluetoothHelper.isNotificationServiceRunning(context)) {
                info_msg += "- " + context.getString(R.string.bluetooth_alert_info_notifications) + "\n"
            }
            if (prefs.bluetooth_only_on_connect.get() && prefs.bluetooth_devices.get().isEmpty()) {
                info_msg += "- " + context.getString(R.string.bluetooth_alert_info_device) + "\n"
            }
            if (prefs.bluetooth_apps.get().isEmpty()) {
                info_msg += "- " + context.getString(R.string.bluetooth_alert_info_apps) + "\n"
            }
            if (prefs.bluetooth_save_read.get() && !prefs.bluetooth_delayed_read.get() && Build.MANUFACTURER.equals("samsung", ignoreCase = true) && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                info_msg += "- " + context.getString(R.string.bluetooth_alert_info_markasread) + "\n"
            }

            if (info_msg != "") {
                AlertDialog.Builder(activity!!)
                        .setTitle("Information")
                        .setMessage(info_msg.trim())
                        .setPositiveButton("Okay", null)
                        .show()
            }
        }
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
        var local_bluetooth_enabled = state.bluetooth_enabled
        var local_bluetooth_tethering = state.bluetooth_tethering
        var local_bluetooth_whatsapp_to_contact = state.bluetooth_whatsapp_to_contact

        if(!state.bluetooth_enabled) {
            BluetoothHelper.deleteBluetoothMessages(context, false)
        }

        //Forwarding enabled but not default SMS-App or has no Notification-Access
        if(state.bluetooth_enabled and (!Utils.isDefaultSmsApp(context) or !BluetoothHelper.hasNotificationAccess(context))) {
            prefs.bluetooth_enabled.set(false)
            local_bluetooth_enabled = false
            navigator.showBluetoothAccess()
        }

        //WhatsApp-to-Contact enabled but no Contact-Permission
        if(state.bluetooth_whatsapp_to_contact && !BluetoothHelper.hasContactPermission(context)) {
            prefs.bluetooth_whatsapp_to_contact.set(false)
            local_bluetooth_whatsapp_to_contact = false
            BluetoothHelper.requestContactPermission(activity!!)
        }

        //Tethering enabled but no system-write permission
        if(state.bluetooth_tethering && Build.VERSION.SDK_INT >= 23) {
            if(!Settings.System.canWrite(context)) {
                prefs.bluetooth_tethering.set(false)
                local_bluetooth_tethering = false

                val intent_write_settings = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent_write_settings.data = Uri.parse("package:" + context.packageName)
                intent_write_settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                AlertDialog.Builder(activity!!)
                        .setTitle(R.string.main_permission_required)
                        .setMessage(String.format(context.getString(R.string.settings_bluetooth_tethering_dialog), context.getString(R.string.app_name)))
                        .setPositiveButton(R.string.title_settings) { _, _ -> startActivity(Intent(intent_write_settings)) }
                        .setNegativeButton(R.string.button_cancel, null)
                        .show()
            }
        }

        bluetooth_menu_main.setVisible(state.bluetooth_enabled)
        bluetooth_select_device.setVisible(state.bluetooth_only_on_connect)
        bluetooth_autodelete.setVisible(state.bluetooth_only_on_connect)

        bluetooth_delayed_read.setVisible(state.bluetooth_save_read)

        bluetooth_whatsapp_blocked_group.setVisible(local_bluetooth_whatsapp_to_contact)
        bluetooth_whatsapp_blocked_contact.setVisible(local_bluetooth_whatsapp_to_contact)
        bluetooth_whatsapp_hide_prefix.setVisible(local_bluetooth_whatsapp_to_contact)

        bluetooth_more_divider.setVisible(state.bluetooth_only_on_connect)
        bluetooth_more_cat.setVisible(state.bluetooth_only_on_connect)
        bluetooth_max_vol.setVisible(state.bluetooth_only_on_connect)
        bluetooth_tethering.setVisible(state.bluetooth_only_on_connect)

        bluetooth_battery.setVisible(state.bluetooth_enabled)

        bluetooth_enabled.checkbox.isChecked = local_bluetooth_enabled
        bluetooth_only_on_connect.checkbox.isChecked = state.bluetooth_only_on_connect
        bluetooth_autodelete.checkbox.isChecked = state.bluetooth_autodelete
        bluetooth_save_read.checkbox.isChecked = state.bluetooth_save_read
        bluetooth_delayed_read.checkbox.isChecked = state.bluetooth_delayed_read
        bluetooth_emoji.checkbox.isChecked = state.bluetooth_emoji
        bluetooth_appname_as_sender_text.checkbox.isChecked = state.bluetooth_appname_as_sender_text
        bluetooth_appname_as_sender_number.checkbox.isChecked = state.bluetooth_appname_as_sender_number
        bluetooth_whatsapp_to_contact.checkbox.isChecked = local_bluetooth_whatsapp_to_contact
        bluetooth_whatsapp_hide_prefix.checkbox.isChecked = state.bluetooth_whatsapp_hide_prefix
        bluetooth_max_vol.checkbox.isChecked = state.bluetooth_max_vol
        bluetooth_tethering.checkbox.isChecked = local_bluetooth_tethering

        //Connected and Last-Connected-Device and Time available
        if (prefs.bluetooth_current_status.get() &&
            prefs.bluetooth_last_connect.get() > 0L &&
            prefs.bluetooth_last_connect_device.get() != "" &&
            state.bluetooth_only_on_connect)
        {
            bluetooth_device_status.setVisible(true)
            bluetooth_device_status.title = String.format(context.getString(R.string.settings_bluetooth_device_status_connected), prefs.bluetooth_last_connect_device.get(), SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(prefs.bluetooth_last_connect.get()))
        }
        //Not Connected and Last-Connected-Device and Time available
        else if (!prefs.bluetooth_current_status.get() &&
                    prefs.bluetooth_last_disconnect.get() > 0L &&
                    prefs.bluetooth_last_connect_device.get() != "" &&
                    state.bluetooth_only_on_connect)
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
                .setPositiveButton(R.string.title_settings) { _, _ -> BluetoothBatteryUtils.startPowerSaverIntent(activity!!) }
                .setNegativeButton(R.string.button_cancel, null)
                .show()
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

    override fun showBluetoothWhatsAppBlockedContact() {
        val items = prefs.bluetooth_whatsapp_blocked_contact.get()
        val array = arrayOfNulls<String>(items.size)
        items.toHashSet().toArray(array)

        val editText = EditText(activity!!)

        AlertDialog.Builder(activity!!)
                .setTitle(R.string.settings_bluetooth_block_whatsapp_contact_title)
                .setItems(array) { _, which ->
                    AlertDialog.Builder(activity!!)
                            .setTitle(R.string.settings_bluetooth_block_whatsapp_unblock_confirm)
                            .setCancelable(false)
                            .setMessage(context.getString(R.string.settings_bluetooth_block_whatsapp_unblock_summary, array[which]))
                            .setPositiveButton(R.string.button_yes) { _, _ ->
                                BluetoothWABlocked.setWAUnblock(context, array[which], false)
                                //Toast.makeText(context, "Unblock: " + array[which], Toast.LENGTH_LONG).show()
                            }
                            .setNegativeButton(R.string.button_no) { dialog, _ -> dialog.cancel() }
                            .show()
                }
                .setPositiveButton(R.string.button_add) { _, _ ->
                    AlertDialog.Builder(activity!!)
                            .setTitle(R.string.settings_bluetooth_block_whatsapp_contact_add_title)
                            .setCancelable(false)
                            .setMessage(R.string.settings_bluetooth_block_whatsapp_contact_add_summary)
                            .setView(editText)
                            .setPositiveButton(R.string.button_add) { _, _ ->
                                if(editText.text.isNotEmpty()) {
                                    BluetoothWABlocked.setWABlock(context, PhoneNumberUtils.stripSeparators(editText.text.toString()), false)
                                    //Toast.makeText(context, "Add " + PhoneNumberUtils.stripSeparators(editText.text.toString()), Toast.LENGTH_LONG).show()
                                }
                            }
                            .setNegativeButton(R.string.button_cancel) { dialog, _ -> dialog.cancel() }
                            .show()
                }
                .setNegativeButton(R.string.button_cancel) { dialog, _ -> dialog.cancel() }
                .show()
    }

    override fun showBluetoothWhatsAppBlockedGroup() {
        val items = prefs.bluetooth_whatsapp_blocked_group.get()
        val array = arrayOfNulls<String>(items.size)
        items.toHashSet().toArray(array)

        val editText = EditText(activity!!)

        AlertDialog.Builder(activity!!)
                .setTitle(R.string.settings_bluetooth_block_whatsapp_group_title)
                .setItems(array) { _, which ->
                    AlertDialog.Builder(activity!!)
                            .setTitle(R.string.settings_bluetooth_block_whatsapp_unblock_confirm)
                            .setCancelable(false)
                            .setMessage(context.getString(R.string.settings_bluetooth_block_whatsapp_unblock_summary, array[which]))
                            .setPositiveButton(R.string.button_yes) { _, _ ->
                                BluetoothWABlocked.setWAUnblock(context, array[which], true)
                                //Toast.makeText(context, "Unblock: " + array[which], Toast.LENGTH_LONG).show()
                            }
                            .setNegativeButton(R.string.button_no) { dialog, _ -> dialog.cancel() }
                            .show()
                }
                .setPositiveButton(R.string.button_add) { _, _ ->
                    AlertDialog.Builder(activity!!)
                            .setTitle(R.string.settings_bluetooth_block_whatsapp_group_add_title)
                            .setCancelable(false)
                            .setMessage(R.string.settings_bluetooth_block_whatsapp_group_add_summary)
                            .setView(editText)
                            .setPositiveButton(R.string.button_add) { _, _ ->
                                if(editText.text.isNotEmpty()) {
                                    BluetoothWABlocked.setWABlock(context, editText.text.toString(), true)
                                    //Toast.makeText(context, "Add: " + editText.text.toString(), Toast.LENGTH_LONG).show()
                                }
                            }
                            .setNegativeButton(R.string.button_cancel) { dialog, _ -> dialog.cancel() }
                            .show()
                }
                .setNegativeButton(R.string.button_cancel) { dialog, _ -> dialog.cancel() }
                .show()
    }


}