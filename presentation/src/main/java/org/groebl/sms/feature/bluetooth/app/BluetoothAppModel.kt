package org.groebl.sms.feature.bluetooth.app

import android.content.pm.ApplicationInfo


class BluetoothAppModel(AppName:String = "", AppApkName:String = "", AppInfo: ApplicationInfo) {
    var appName = AppName
    var appInfo = AppInfo
    var appApkName = AppApkName
}