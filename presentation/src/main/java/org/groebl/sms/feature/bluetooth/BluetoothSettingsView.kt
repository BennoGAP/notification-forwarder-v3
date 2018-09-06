package org.groebl.sms.feature.bluetooth

import org.groebl.sms.common.base.QkViewContract
import org.groebl.sms.common.widget.PreferenceView
import io.reactivex.Observable

interface BluetoothSettingsView : QkViewContract<BluetoothSettingsState> {
    fun preferenceMainClicks(): Observable<PreferenceView>
    fun preferenceFullClicks(): Observable<PreferenceView>

    fun showBluetoothApps()
    fun showBluetoothDevices()
    fun showBluetoothAbout()

    fun showBluetoothWhatsAppBlockedContact()
    fun showBluetoothWhatsAppBlockedGroup()
}