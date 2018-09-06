package org.groebl.sms.feature.bluetooth.app

import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.groebl.sms.BuildConfig
import org.groebl.sms.R
import org.groebl.sms.common.base.QkThemedActivity
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.bluetooth_apps_activity.*
import java.util.*
import javax.inject.Inject

class BluetoothAppActivity : QkThemedActivity(), BluetoothAppView {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by lazy { ViewModelProviders.of(this, viewModelFactory)[BluetoothAppViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth_apps_activity)
        setTitle(R.string.settings_bluetooth_apps_title)
        showBackButton(true)
        viewModel.bindView(this)

        listapps.adapter = BluetoothAppAdapter(scanningInstalled(), prefs.bluetooth_apps.get().toHashSet(), prefs)
    }

    override fun render(state: BluetoothAppState) {
        // No special rendering required
    }


    fun scanningInstalled(): ArrayList<BluetoothAppModel>{
        var packageModel = ArrayList<BluetoothAppModel>()

        var installedApps = packageManager.getInstalledPackages(0)
        for (apps in installedApps) {
            if(!packageManager(apps)) {
                packageModel.add(BluetoothAppModel(apps.applicationInfo.loadLabel(packageManager).toString(), apps.packageName, apps.applicationInfo.loadIcon(packageManager)))
            }
        }

        packageModel.sortBy { it.appName.toLowerCase() }

        return packageModel
    }
    private fun packageManager(appinfo: PackageInfo):Boolean{
        return appinfo.applicationInfo.icon.equals(0) || appinfo.packageName.equals(BuildConfig.APPLICATION_ID, true)
    }

}