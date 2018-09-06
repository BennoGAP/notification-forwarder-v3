package org.groebl.sms.feature.bluetooth

import android.content.Context
import org.groebl.sms.R
import org.groebl.sms.common.Navigator
import org.groebl.sms.common.base.QkPresenter
import org.groebl.sms.util.Preferences
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.rxkotlin.plusAssign
import timber.log.Timber
import javax.inject.Inject

class BluetoothSettingsPresenter @Inject constructor(
        private val context: Context,
        private val navigator: Navigator,
        private val prefs: Preferences
) : QkPresenter<BluetoothSettingsView, BluetoothSettingsState>(BluetoothSettingsState()) {

    init {

        disposables += prefs.bluetooth_enabled.asObservable()
                .subscribe { enabled -> newState { copy(bluetooth_enabled = enabled) } }

        disposables += prefs.bluetooth_only_on_connect.asObservable()
                .subscribe { enabled -> newState { copy(bluetooth_only_on_connect = enabled) } }

        disposables += prefs.bluetooth_autodelete.asObservable()
                .subscribe { enabled -> newState { copy(bluetooth_autodelete = enabled) } }

        disposables += prefs.bluetooth_save_read.asObservable()
                .subscribe { enabled -> newState { copy(bluetooth_save_read = enabled) } }

        disposables += prefs.bluetooth_delayed_read.asObservable()
                .subscribe { enabled -> newState { copy(bluetooth_delayed_read = enabled) } }

        disposables += prefs.bluetooth_emoji.asObservable()
                .subscribe { enabled -> newState { copy(bluetooth_emoji = enabled) } }

        disposables += prefs.bluetooth_appname_as_sender_text.asObservable()
                .subscribe { enabled -> newState { copy(bluetooth_appname_as_sender_text = enabled) } }

        disposables += prefs.bluetooth_appname_as_sender_number.asObservable()
                .subscribe { enabled -> newState { copy(bluetooth_appname_as_sender_number = enabled) } }

        disposables += prefs.bluetooth_whatsapp_to_contact.asObservable()
                .subscribe { enabled -> newState { copy(bluetooth_whatsapp_to_contact = enabled) } }

        disposables += prefs.bluetooth_whatsapp_hide_prefix.asObservable()
                .subscribe { enabled -> newState { copy(bluetooth_whatsapp_hide_prefix = enabled) } }

        disposables += prefs.bluetooth_max_vol.asObservable()
                .subscribe { enabled -> newState { copy(bluetooth_max_vol = enabled) } }

    }

    override fun bindIntents(view: BluetoothSettingsView) {
        super.bindIntents(view)

        view.preferenceFullClicks()
                .autoDisposable(view.scope())
                .subscribe {
                    Timber.v("Preference-Full click: ${context.resources.getResourceName(it.id)}")

                    when (it.id) {
                        R.id.bluetooth_enabled -> {
                            prefs.bluetooth_enabled.set(!prefs.bluetooth_enabled.get())
                        } //TODO //(when (!prefs.bluetooth_enabled.get() and (!isDefaultSmsApp(context) or !hasNotificationAccess(context))) { true -> navigator.showBluetoothAccess() else -> prefs.bluetooth_enabled.set(!prefs.bluetooth_enabled.get())} )
                        R.id.bluetooth_notification_access -> navigator.showNotificationAccess()
                        R.id.bluetooth_faq-> navigator.showFAQ()
                        R.id.bluetooth_donate-> navigator.showDonationBluetooth()
                        R.id.bluetooth_about -> view.showBluetoothAbout()
                    }
                }


        view.preferenceMainClicks()
                .autoDisposable(view.scope())
                .subscribe {
                    Timber.v("Preference-Main click: ${context.resources.getResourceName(it.id)}")

                    when (it.id) {
                        R.id.bluetooth_only_on_connect -> prefs.bluetooth_only_on_connect.set(!prefs.bluetooth_only_on_connect.get())
                        R.id.bluetooth_autodelete -> prefs.bluetooth_autodelete.set(!prefs.bluetooth_autodelete.get())
                        R.id.bluetooth_select_device -> view.showBluetoothDevices()
                        R.id.bluetooth_allowed_apps -> view.showBluetoothApps()
                        R.id.bluetooth_save_read -> prefs.bluetooth_save_read.set(!prefs.bluetooth_save_read.get())
                        R.id.bluetooth_delayed_read -> prefs.bluetooth_delayed_read.set(!prefs.bluetooth_delayed_read.get())
                        R.id.bluetooth_emoji -> prefs.bluetooth_emoji.set(!prefs.bluetooth_emoji.get())
                        R.id.bluetooth_appname_as_sender_text -> {
                            if(prefs.bluetooth_appname_as_sender_text.get()) {
                                prefs.bluetooth_appname_as_sender_text.set(false)
                            } else {
                                prefs.bluetooth_appname_as_sender_text.set(true)
                                prefs.bluetooth_appname_as_sender_number.set(false)
                            }
                        }
                        R.id.bluetooth_appname_as_sender_number -> {
                            if(prefs.bluetooth_appname_as_sender_number.get()) {
                                prefs.bluetooth_appname_as_sender_number.set(false)
                            } else {
                                prefs.bluetooth_appname_as_sender_number.set(true)
                                prefs.bluetooth_appname_as_sender_text.set(false)
                            }
                        }
                        R.id.bluetooth_whatsapp_to_contact -> prefs.bluetooth_whatsapp_to_contact.set(!prefs.bluetooth_whatsapp_to_contact.get())
                        R.id.bluetooth_whatsapp_hide_prefix -> prefs.bluetooth_whatsapp_hide_prefix.set(!prefs.bluetooth_whatsapp_hide_prefix.get())
                        R.id.bluetooth_whatsapp_blocked_group -> view.showBluetoothWhatsAppBlockedGroup()
                        R.id.bluetooth_whatsapp_blocked_contact -> view.showBluetoothWhatsAppBlockedContact()
                        R.id.bluetooth_max_vol -> prefs.bluetooth_max_vol.set(!prefs.bluetooth_max_vol.get())

                    }
                }
    }


}