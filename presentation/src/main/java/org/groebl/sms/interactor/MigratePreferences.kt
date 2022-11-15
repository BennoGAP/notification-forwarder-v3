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
package org.groebl.sms.interactor

import android.bluetooth.BluetoothAdapter
import com.f2prateek.rx.preferences2.RxSharedPreferences
import org.groebl.sms.util.NightModeManager
import org.groebl.sms.util.Preferences
import io.reactivex.Flowable
import timber.log.Timber
import javax.inject.Inject

/**
 * When upgrading from 2.7.3 to 3.0, migrate the preferences
 *
 * Blocked conversations will be migrated in SyncManager
 */
class MigratePreferences @Inject constructor(
        private val nightModeManager: NightModeManager,
        private val prefs: Preferences,
        private val rxPrefs: RxSharedPreferences
) : Interactor<Unit>() {

    override fun buildObservable(params: Unit): Flowable<*> {
        return Flowable.fromCallable { rxPrefs.getBoolean("pref_key_welcome_seen", false) }
                .filter { seen -> seen.get() } // Only proceed if this value is true. It will be set false at the end
                .doOnNext {
                    // Theme
                    val defaultTheme = prefs.theme().get().toString()
                    val oldTheme = rxPrefs.getString("pref_key_theme", defaultTheme).get()
                    prefs.theme().set(Integer.parseInt(oldTheme))

                    // Night mode
                    val background = rxPrefs.getString("pref_key_background", "light").get()
                    val autoNight = rxPrefs.getBoolean("pref_key_night_auto", false).get()
                    when {
                        autoNight -> nightModeManager.updateNightMode(Preferences.NIGHT_MODE_AUTO)
                        background == "light" -> nightModeManager.updateNightMode(Preferences.NIGHT_MODE_OFF)
                        background == "grey" -> {
                            nightModeManager.updateNightMode(Preferences.NIGHT_MODE_OFF)
                            prefs.gray.set(true)
                        }
                        background == "black" -> {
                            nightModeManager.updateNightMode(Preferences.NIGHT_MODE_ON)
                            prefs.black.set(true)
                        }
                    }

                    // Delivery
                    prefs.delivery.set(rxPrefs.getBoolean("pref_key_delivery", prefs.delivery.get()).get())

                    // Quickreply
                    prefs.qkreply.set(rxPrefs.getBoolean("pref_key_quickreply_enabled", prefs.qkreply.get()).get())
                    prefs.qkreplyTapDismiss.set(rxPrefs.getBoolean("pref_key_quickreply_dismiss", prefs.qkreplyTapDismiss.get()).get())

                    // Font size
                    prefs.textSize.set(rxPrefs.getString("pref_key_font_size", "${prefs.textSize.get()}").get().toInt())

                    // Unicode
                    prefs.unicode.set(rxPrefs.getBoolean("pref_key_strip_unicode", prefs.unicode.get()).get())

                    // Bluetooth Settings
                    prefs.bluetooth_enabled.set(rxPrefs.getBoolean("pref_key_bluetooth_enabled", prefs.bluetooth_enabled.get()).get())
                    prefs.bluetooth_only_on_connect.set(rxPrefs.getBoolean("pref_key_bluetooth_connected", prefs.bluetooth_only_on_connect.get()).get())
                    prefs.bluetooth_autodelete.set(rxPrefs.getBoolean("pref_key_bluetooth_delete", prefs.bluetooth_autodelete.get()).get())
                    prefs.bluetooth_save_read.set(rxPrefs.getBoolean("pref_key_bluetooth_markasread", prefs.bluetooth_save_read.get()).get())
                    prefs.bluetooth_delayed_read.set(rxPrefs.getBoolean("pref_key_bluetooth_markasread_delayed", prefs.bluetooth_delayed_read.get()).get())
                    prefs.bluetooth_emoji.set(rxPrefs.getBoolean("pref_key_bluetooth_emoji", prefs.bluetooth_emoji.get()).get())
                    prefs.bluetooth_appname_as_sender_text.set(rxPrefs.getBoolean("pref_key_bluetooth_showname", prefs.bluetooth_appname_as_sender_text.get()).get())
                    prefs.bluetooth_appname_as_sender_number.set(rxPrefs.getBoolean("pref_key_bluetooth_nametonumber", prefs.bluetooth_appname_as_sender_number.get()).get())
                    prefs.bluetooth_whatsapp_to_contact.set(rxPrefs.getBoolean("pref_key_bluetooth_whatsapp_magic", prefs.bluetooth_whatsapp_to_contact.get()).get())
                    prefs.bluetooth_whatsapp_hide_prefix.set(rxPrefs.getBoolean("pref_key_bluetooth_whatsapp_noprefix", prefs.bluetooth_whatsapp_hide_prefix.get()).get())
                    prefs.bluetooth_max_vol.set(rxPrefs.getBoolean("pref_key_bluetooth_maxvol", prefs.bluetooth_max_vol.get()).get())
                    prefs.bluetooth_apps.set(rxPrefs.getStringSet("pref_key_bluetooth_apps").get())
                    prefs.bluetooth_whatsapp_blocked_group.set(rxPrefs.getStringSet("pref_key_block_whatsapp").get())

                    try {
                        val oldBluetoothDevices = rxPrefs.getStringSet("pref_key_bluetooth_devices").get()
                        var macAddress: MutableSet<String> = mutableSetOf()
                        val pairedDevices = BluetoothAdapter.getDefaultAdapter().bondedDevices
                        for (bt in pairedDevices) {
                            when { oldBluetoothDevices.contains(bt.name) -> macAddress.add(bt.address)
                            }
                        }
                        prefs.bluetooth_devices.set(macAddress)
                    } catch (e: Exception) {
                        Timber.e(e)
                    }

                    rxPrefs.getBoolean("pref_key_bluetooth_enabled").delete()
                    rxPrefs.getBoolean("pref_key_bluetooth_connected").delete()
                    rxPrefs.getBoolean("pref_key_bluetooth_delete").delete()
                    rxPrefs.getBoolean("pref_key_bluetooth_markasread").delete()
                    rxPrefs.getBoolean("pref_key_bluetooth_markasread_delayed").delete()
                    rxPrefs.getBoolean("pref_key_bluetooth_emoji").delete()
                    rxPrefs.getBoolean("pref_key_bluetooth_showname").delete()
                    rxPrefs.getBoolean("pref_key_bluetooth_nametonumber").delete()
                    rxPrefs.getBoolean("pref_key_bluetooth_whatsapp_magic").delete()
                    rxPrefs.getBoolean("pref_key_bluetooth_whatsapp_noprefix").delete()
                    rxPrefs.getBoolean("pref_key_bluetooth_maxvol").delete()
                    rxPrefs.getBoolean("pref_key_bluetooth_current_status").delete()
                    rxPrefs.getStringSet("pref_key_bluetooth_apps").delete()
                    rxPrefs.getStringSet("pref_key_bluetooth_devices").delete()
                    rxPrefs.getStringSet("pref_key_block_whatsapp").delete()

                }
                .doOnNext { seen -> seen.delete() } // Clear this value so that we don't need to migrate again
    }

}