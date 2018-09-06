package org.groebl.sms.feature.bluetooth.device

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.groebl.sms.R
import org.groebl.sms.common.base.QkThemedActivity
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.bluetooth_devices_activity.*
import java.util.*
import javax.inject.Inject

class BluetoothDeviceActivity  : QkThemedActivity(), BluetoothDeviceView {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by lazy { ViewModelProviders.of(this, viewModelFactory)[BluetoothDeviceViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth_devices_activity)
        setTitle(R.string.settings_bluetooth_devices_title)
        showBackButton(true)
        viewModel.bindView(this)

        listdevices.adapter = BluetoothDeviceAdapter(getBondedDevices(), prefs.bluetooth_devices.get().toHashSet(), prefs)
    }

    override fun render(state: BluetoothDeviceState) {

    }

    fun getBondedDevices(): ArrayList<BluetoothDeviceModel> {
        empty.text = ""
        //BMW 5xxx7 - 9C:DF:03:xx:xx:xx
        var packageModel = ArrayList<BluetoothDeviceModel>()
        val blAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = blAdapter.bondedDevices
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                packageModel.add(BluetoothDeviceModel(device.name, device.address))
            }
            packageModel.sortBy { it.deviceName.toLowerCase() }
        } else {
            empty.text = getString(R.string.settings_bluetooth_no_devices)
        }

        return packageModel
    }

}