package org.groebl.sms.feature.bluetooth.app

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import org.groebl.sms.R
import org.groebl.sms.util.Preferences
import java.util.*

class BluetoothAppAdapter(val data: ArrayList<BluetoothAppModel>, val prefs: Preferences, val context: Context): RecyclerView.Adapter<BluetoothAppAdapter.CustomViewHolder>() {

    private val allowedApps = prefs.bluetooth_apps.get().toHashSet()

    private val mPm: PackageManager = context.packageManager

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        return CustomViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.bluetooth_apps_list_item, parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val dataModel = data[position]
        val isChecked = allowedApps.contains(dataModel.appApkName)
        holder.appName.text = dataModel.appName
        holder.appIcon.setImageDrawable(dataModel.appInfo.loadIcon(mPm))
        holder.containerView.tag = dataModel.appApkName
        holder.appCheckBox.isChecked = isChecked

        when (isChecked) {
            false -> {  holder.appIcon.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }); }
        }

        holder.appCheckBox.setOnClickListener { toggleSelection(holder) }
        holder.containerView.setOnClickListener { holder.appCheckBox.isChecked = !holder.appCheckBox.isChecked; toggleSelection(holder)  }
    }

    private fun toggleSelection(holder: CustomViewHolder) {
        when (allowedApps.contains(holder.containerView.tag.toString())) {
            true -> { allowedApps.remove(holder.containerView.tag.toString()); holder.appIcon.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }); }
            false -> { allowedApps.add(holder.containerView.tag.toString()); holder.appIcon.clearColorFilter(); }
        }

        prefs.bluetooth_apps.set(allowedApps)
    }

    class CustomViewHolder(itemView: View):RecyclerView.ViewHolder(itemView), LayoutContainer {
        var appName = itemView.findViewById<TextView>(R.id.appTitle)
        var appIcon = itemView.findViewById<ImageView>(R.id.appIcon)
        var appCheckBox = itemView.findViewById<CheckBox>(R.id.appCheckBox)
        override val containerView: View = itemView
    }
}