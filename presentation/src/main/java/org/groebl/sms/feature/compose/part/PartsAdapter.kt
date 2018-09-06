/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
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
package org.groebl.sms.feature.compose.part

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.groebl.sms.common.Navigator
import org.groebl.sms.common.base.QkAdapter
import org.groebl.sms.common.base.QkViewHolder
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.extensions.forwardTouches
import org.groebl.sms.extensions.isImage
import org.groebl.sms.extensions.isVCard
import org.groebl.sms.extensions.isVideo
import org.groebl.sms.feature.compose.BubbleUtils.canGroup
import org.groebl.sms.model.Message
import org.groebl.sms.model.MmsPart
import kotlinx.android.synthetic.main.message_list_item_in.view.*

class PartsAdapter(context: Context, navigator: Navigator, theme: Colors.Theme) : QkAdapter<MmsPart>() {

    private val partBinders = listOf(
            MediaBinder(context, navigator),
            VCardBinder(context, navigator, theme)
    )

    private lateinit var message: Message
    private var previous: Message? = null
    private var next: Message? = null
    private var messageView: View? = null
    private var bodyVisible: Boolean = true

    fun setData(message: Message, previous: Message?, next: Message?, messageView: View) {
        this.message = message
        this.previous = previous
        this.next = next
        this.messageView = messageView
        this.bodyVisible = messageView.body.visibility == View.VISIBLE
        this.data = message.parts.filter { it.isImage() || it.isVideo() || it.isVCard() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val layout = partBinders.getOrNull(viewType)?.partLayout ?: 0
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        messageView?.let(view::forwardTouches)
        return QkViewHolder(view)
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val part = data[position]
        val view = holder.itemView

        val canGroupWithPrevious = canGroup(message, previous) || position > 0
        val canGroupWithNext = canGroup(message, next) || position < itemCount - 1 || bodyVisible

        partBinders
                .firstOrNull { it.canBindPart(part) }
                ?.bindPart(view, part, message, canGroupWithPrevious, canGroupWithNext)
    }

    override fun getItemViewType(position: Int): Int {
        val part = data[position]
        return partBinders.indexOfFirst { it.canBindPart(part) }
    }

}