package org.groebl.sms.feature.bluetooth.device

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.bluetooth_devices_activity.*
import org.groebl.sms.R
import org.groebl.sms.common.base.QkThemedActivity
import java.util.*
import javax.inject.Inject

class BluetoothDeviceActivity  : QkThemedActivity(), BluetoothDeviceView {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by lazy { ViewModelProvider(this, viewModelFactory)[BluetoothDeviceViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth_devices_activity)
        setTitle(R.string.settings_bluetooth_devices_title)
        showBackButton(true)
        viewModel.bindView(this)

        listdevices.adapter = BluetoothDeviceAdapter(getBondedDevices(), prefs)
    }

    override fun render(state: BluetoothDeviceState) {

    }

    fun getBondedDevices(): ArrayList<BluetoothDeviceModel> {
        empty.text = ""
        var packageModel = ArrayList<BluetoothDeviceModel>()
        var checkedDevices =  prefs.bluetooth_devices.get()
        var newCheckedDevices: MutableSet<String> = mutableSetOf()
        try {
            val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val pairedDevices = bluetoothManager.adapter.bondedDevices

            if (!bluetoothManager.adapter.isEnabled) {
                empty.text = getString(R.string.settings_bluetooth_disabled)
            } else if (pairedDevices.size > 0) {
                for (device in pairedDevices) {
                    packageModel.add(BluetoothDeviceModel(device.name, device.address, checkedDevices.contains(device.address)))

                    if(checkedDevices.contains(device.address)) { newCheckedDevices.add(device.address) }
                }

                packageModel.sortBy { it.deviceName.lowercase(Locale.getDefault()) }

                //Only paired Devices should be saved in the prefs
                prefs.bluetooth_devices.set(newCheckedDevices)

            } else {
                empty.text = getString(R.string.settings_bluetooth_no_devices)
            }

        } catch (e: Exception) {
            empty.text = getString(R.string.settings_bluetooth_no_devices)
        }

        return packageModel
    }

}