/*
 * Copyright (C) 2019 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.groebl.sms.feature.compose.editing

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import org.groebl.sms.R
import org.groebl.sms.common.base.QkAdapter
import org.groebl.sms.common.base.QkViewHolder
import org.groebl.sms.extensions.Optional
import org.groebl.sms.model.PhoneNumber
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.phone_number_list_item.*
import kotlinx.android.synthetic.main.radio_preference_view.*
import kotlinx.android.synthetic.main.radio_preference_view.view.*
import org.groebl.sms.common.util.extensions.forwardTouches
import javax.inject.Inject

class PhoneNumberPickerAdapter @Inject constructor(
    private val context: Context
) : QkAdapter<PhoneNumber>() {

    val selectedItemChanges: Subject<Optional<Long>> = BehaviorSubject.create()

    private var selectedItem: Long? = null
        set(value) {
            data.indexOfFirst { number -> number.id == field }.takeIf { it != -1 }?.run(::notifyItemChanged)
            field = value
            data.indexOfFirst { number -> number.id == field }.takeIf { it != -1 }?.run(::notifyItemChanged)
            selectedItemChanges.onNext(Optional(value))
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.phone_number_list_item, parent, false)
        return QkViewHolder(view).apply {
            radioButton.forwardTouches(itemView)

            view.setOnClickListener {
                val phoneNumber = getItem(adapterPosition)
                selectedItem = phoneNumber.id
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val phoneNumber = getItem(position)

        holder.number.radioButton.isChecked = phoneNumber.id == selectedItem
        holder.number.titleView.text = phoneNumber.address
        holder.number.summaryView.text = when (phoneNumber.isDefault) {
            true -> context.getString(R.string.compose_number_picker_default, phoneNumber.type)
            false -> phoneNumber.type
        }
    }

    override fun onDatasetChanged() {
        super.onDatasetChanged()
        selectedItem = data.find { number -> number.isDefault }?.id ?: data.firstOrNull()?.id
    }

}