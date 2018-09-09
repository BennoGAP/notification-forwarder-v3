package org.groebl.sms.feature.bluetooth.device

class BluetoothDeviceModel(DeviceName:String = "", DeviceMac:String = "", isChecked: Boolean = false) {
    var deviceName = DeviceName
    var deviceMac = DeviceMac
    var checked = isChecked
}