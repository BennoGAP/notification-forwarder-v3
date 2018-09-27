package org.groebl.sms.feature.bluetooth.device

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.groebl.sms.R
import org.groebl.sms.util.Preferences
import java.util.*

class BluetoothDeviceAdapter(val data: ArrayList<BluetoothDeviceModel>, val prefs: Preferences) : RecyclerView.Adapter<BluetoothDeviceAdapter.CustomViewHolder>() {

    private val allowedDevices = prefs.bluetooth_devices.get().toHashSet()

     override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothDeviceAdapter.CustomViewHolder {
        return BluetoothDeviceAdapter.CustomViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.bluetooth_devices_list_item, parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val dataModel = data[position]
        holder.deviceName.text = dataModel.deviceName + "\n(" + dataModel.deviceMac + ")"
        holder.itemView.tag = dataModel.deviceMac
        holder.deviceCheckBox.isChecked = dataModel.checked

        when (dataModel.checked) {
            false -> { holder.deviceBluetoothIcon.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }); }
        }

        holder.deviceCheckBox.setOnClickListener { toggleSelection(holder) }
        holder.itemView.setOnClickListener { holder.deviceCheckBox.isChecked = !holder.deviceCheckBox.isChecked; toggleSelection(holder)  }
    }

    private fun toggleSelection(holder: BluetoothDeviceAdapter.CustomViewHolder) {
        when (allowedDevices.contains(holder.itemView.tag.toString())) {
            true -> {
                allowedDevices.remove(holder.itemView.tag.toString())
                holder.deviceBluetoothIcon.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })

                //Set connection status to disconnected when device get unselected
                val parts = holder.deviceName.text.split("\n")
                if (prefs.bluetooth_current_status.get() && parts[0] == prefs.bluetooth_last_connect_device.get()) {
                    prefs.bluetooth_current_status.set(false)
                    prefs.bluetooth_last_disconnect.set(System.currentTimeMillis())
                }
            }
            false -> {
                allowedDevices.add(holder.itemView.tag.toString())
                holder.deviceBluetoothIcon.clearColorFilter()
            }
        }

        //println(holder.itemView.tag.toString() + " - new val: " + holder.appCheckBox.isChecked.toString())
        //val array = arrayOfNulls<String>(allowedDevices.size)
        //allowedDevices.toHashSet().toArray(array)
        //println(Arrays.toString(array))
        prefs.bluetooth_devices.set(allowedDevices)
    }

    class CustomViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        var deviceName = itemView.findViewById<TextView>(R.id.title)
        var deviceCheckBox = itemView.findViewById<CheckBox>(R.id.checkBox)
        var deviceBluetoothIcon = itemView.findViewById<ImageView>(R.id.bluetoothIcon)
    }

}