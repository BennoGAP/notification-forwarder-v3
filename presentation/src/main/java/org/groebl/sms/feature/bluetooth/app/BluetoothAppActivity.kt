package org.groebl.sms.feature.bluetooth.app

import android.app.ProgressDialog
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.bluetooth_apps_activity.*
import org.groebl.sms.BuildConfig
import org.groebl.sms.R
import org.groebl.sms.common.base.QkThemedActivity
import java.util.*
import javax.inject.Inject

class BluetoothAppActivity : QkThemedActivity(), BluetoothAppView {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by lazy { ViewModelProviders.of(this, viewModelFactory)[BluetoothAppViewModel::class.java] }
    private var scan = ArrayList<BluetoothAppModel>()


    private inner class LoadApplications(context: Context) : AsyncTask<Void, Int, Boolean>() {

        private val pDialog = ProgressDialog(context).apply {
            setMessage(getString(R.string.settings_bluetooth_apps_loading))
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }

        override fun onPreExecute() {
            pDialog.show()
        }

        override fun onPostExecute(result: Boolean?) {
            if(this@BluetoothAppActivity.isDestroyed) { return }

            listapps.adapter = BluetoothAppAdapter(scan, prefs)

            //TODO - Get rid of this .. "Temporary" Workaround
            Handler(Looper.getMainLooper()).postDelayed({ when {pDialog.isShowing -> pDialog.dismiss() } }, 250)
        }

        override fun doInBackground(vararg params: Void): Boolean? {
            scan = scanningInstalled()
            return null
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth_apps_activity)
        setTitle(R.string.settings_bluetooth_apps_title)
        showBackButton(true)
        viewModel.bindView(this)

        appsSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val scanFiltered = ArrayList<BluetoothAppModel>()

                for(scan_output in scan) {
                    if(scan_output.appName.contains(charSequence, ignoreCase = true)) {
                        scanFiltered.add(scan_output)
                    }
                }

                listapps.adapter = BluetoothAppAdapter(scanFiltered, prefs)
            }

            override fun afterTextChanged(editable: Editable) {

            }
        })

        LoadApplications(this).execute()
    }

    override fun render(state: BluetoothAppState) {
        // No special rendering required
    }


    fun scanningInstalled(): ArrayList<BluetoothAppModel>{
        val packageModel = ArrayList<BluetoothAppModel>()
        val checkedApps =  prefs.bluetooth_apps.get()
        val newCheckedApps: MutableSet<String> = mutableSetOf()
        val installedApps = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        var currentAppCount: Int = 0
        //val allAppCount = installedApps.count()


        for (apps in installedApps) {
            currentAppCount += 1
            //println("Load Apps: $currentAppCount / $allAppCount (${(currentAppCount*100)/allAppCount}%)")
            if(!packageManager(apps)) {
                packageModel.add(BluetoothAppModel(apps.applicationInfo.loadLabel(packageManager).toString(), apps.packageName, apps.applicationInfo.loadIcon(packageManager)))

                if(checkedApps.contains(apps.packageName)) { newCheckedApps.add(apps.packageName) }
            }
        }

        //Only installed Apps should be saved in the prefs
        prefs.bluetooth_apps.set(newCheckedApps)

        packageModel.sortBy { it.appName.toLowerCase() }


        return packageModel
    }

    private fun packageManager(appinfo: PackageInfo):Boolean{
        return appinfo.applicationInfo.icon == 0 || appinfo.packageName.equals(BuildConfig.APPLICATION_ID, true)
    }

    private fun hasLaunchIntent(pkgInfo: PackageInfo): Boolean {
        return packageManager.getLaunchIntentForPackage(pkgInfo.applicationInfo.packageName) != null
    }

}