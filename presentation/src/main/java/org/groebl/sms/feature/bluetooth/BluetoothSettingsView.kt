package org.groebl.sms.feature.bluetooth

import io.reactivex.Observable
import org.groebl.sms.common.base.QkViewContract
import org.groebl.sms.common.widget.PreferenceView

interface BluetoothSettingsView : QkViewContract<BluetoothSettingsState> {
    fun preferenceMainClicks(): Observable<PreferenceView>
    fun preferenceFullClicks(): Observable<PreferenceView>

    fun showBluetoothApps()
    fun showBluetoothDevices()
    fun showBluetoothAbout()
    fun showBluetoothBatteryOptimize()
    fun showBluetoothDonate()
    fun showNotificationAccess()

    fun requestDefaultSms()

    fun showBluetoothBlockedContactWhatsApp()
    fun showBluetoothBlockedContactByName(MessengerType: String)
    fun showBluetoothBlockedGroup(MessengerType: String)
}