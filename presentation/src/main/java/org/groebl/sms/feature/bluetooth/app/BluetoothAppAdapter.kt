package org.groebl.sms.feature.bluetooth.app

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

class BluetoothAppAdapter(val data: ArrayList<BluetoothAppModel>, val allowedApps: MutableSet<String>, val prefs: Preferences): RecyclerView.Adapter<BluetoothAppAdapter.CustomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothAppAdapter.CustomViewHolder {
        return BluetoothAppAdapter.CustomViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.bluetooth_apps_list_item, parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        var dataModel = data[position]
        holder.appName.text = dataModel.appName
        holder.appIcon.setImageDrawable(dataModel.appIcon)
        holder.itemView.tag = dataModel.appApkName
        holder.appCheckBox.isChecked = allowedApps.contains(dataModel.appApkName)

        when (allowedApps.contains(dataModel.appApkName)) {
            false -> {  holder.appIcon.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }); }
        }

        holder.appCheckBox.setOnClickListener { toggleSelection(holder) }
        holder.itemView.setOnClickListener { holder.appCheckBox.isChecked = !holder.appCheckBox.isChecked; toggleSelection(holder)  }
    }

    fun toggleSelection(holder: BluetoothAppAdapter.CustomViewHolder) {
        when (allowedApps.contains(holder.itemView.tag.toString())) {
            true -> { allowedApps.remove(holder.itemView.tag.toString()); holder.appIcon.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }); }
            false -> { allowedApps.add(holder.itemView.tag.toString()); holder.appIcon.clearColorFilter(); }
        }

        prefs.bluetooth_apps.set(allowedApps)
    }

    class CustomViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        var appName = itemView.findViewById<TextView>(R.id.appTitle)
        var appIcon = itemView.findViewById<ImageView>(R.id.appIcon)
        var appCheckBox = itemView.findViewById<CheckBox>(R.id.appCheckBox)
    }
}