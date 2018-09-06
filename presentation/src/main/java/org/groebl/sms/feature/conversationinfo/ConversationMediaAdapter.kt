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
package org.groebl.sms.feature.conversationinfo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.groebl.sms.R
import org.groebl.sms.common.Navigator
import org.groebl.sms.common.base.QkRealmAdapter
import org.groebl.sms.common.base.QkViewHolder
import org.groebl.sms.common.util.extensions.setVisible
import org.groebl.sms.extensions.isImage
import org.groebl.sms.extensions.isVideo
import org.groebl.sms.model.MmsPart
import org.groebl.sms.util.GlideApp
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.conversation_media_list_item.view.*
import javax.inject.Inject

class ConversationMediaAdapter @Inject constructor(
        private val context: Context,
        private val navigator: Navigator
) : QkRealmAdapter<MmsPart>() {

    val thumbnailClicks: PublishSubject<View> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.conversation_media_list_item, parent, false)
        return QkViewHolder(view).apply {
            view.thumbnail.setOnClickListener {
                val part = getItem(adapterPosition)!!
                if (part.isImage()) thumbnailClicks.onNext(it) else navigator.showMedia(part.id)
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val part = getItem(position)!!
        val view = holder.itemView

        GlideApp.with(context)
                .load(part.getUri())
                .fitCenter()
                .into(view.thumbnail)

        view.video.setVisible(part.isVideo())
        view.thumbnail.transitionName = part.id.toString()
    }

}