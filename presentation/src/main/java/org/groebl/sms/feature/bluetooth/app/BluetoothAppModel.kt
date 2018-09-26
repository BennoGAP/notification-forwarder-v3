package org.groebl.sms.feature.bluetooth.app

import android.graphics.drawable.Drawable


class BluetoothAppModel(AppName:String = "", AppApkName:String = "", AppIcon: Drawable? = null) {
    var appName = AppName
    var appIcon = AppIcon
    var appApkName = AppApkName
}