package org.groebl.sms.feature.bluetooth.app

import org.groebl.sms.common.base.QkViewModel
import javax.inject.Inject

class BluetoothAppViewModel @Inject constructor(
) : QkViewModel<BluetoothAppView, BluetoothAppState>(BluetoothAppState()) {

    init {
    }

}