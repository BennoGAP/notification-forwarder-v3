package org.groebl.sms.feature.bluetooth.app

import android.graphics.drawable.Drawable


class BluetoothAppModel(AppName:String = "", AppApkName:String = "", AppIcon: Drawable? = null, isChecked: Boolean = false) {
    var appName = AppName
    var appIcon = AppIcon
    var appApkName = AppApkName
    var checked = isChecked
}