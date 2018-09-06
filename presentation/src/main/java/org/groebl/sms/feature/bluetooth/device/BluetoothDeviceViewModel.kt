package org.groebl.sms.feature.bluetooth.device

import org.groebl.sms.common.base.QkViewModel
import javax.inject.Inject

class BluetoothDeviceViewModel @Inject constructor(
) : QkViewModel<BluetoothDeviceView, BluetoothDeviceState>(BluetoothDeviceState()) {

    init {
    }

}