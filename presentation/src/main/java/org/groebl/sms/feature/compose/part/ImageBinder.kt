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
import com.bumptech.glide.Glide
import org.groebl.sms.R
import org.groebl.sms.common.base.QkViewHolder
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.extensions.setVisible
import org.groebl.sms.common.widget.BubbleImageView
import org.groebl.sms.extensions.isImage
import org.groebl.sms.extensions.isVideo
import org.groebl.sms.model.Message
import org.groebl.sms.model.MmsPart
import kotlinx.android.synthetic.main.mms_image_preview_list_item.*
import org.groebl.sms.util.tryOrNull
import javax.inject.Inject

class ImageBinder @Inject constructor(colors: Colors, private val context: Context) : PartBinder() {

    override val partLayout = R.layout.mms_image_preview_list_item
    override var theme = colors.theme()

    override fun canBindPart(part: MmsPart) = part.isImage() || part.isVideo()

    override fun bindPart(
        holder: QkViewHolder,
        part: MmsPart,
        message: Message,
        canGroupWithPrevious: Boolean,
        canGroupWithNext: Boolean
    ) {
        holder.video.setVisible(part.isVideo())
        holder.containerView.setOnClickListener { clicks.onNext(part.id) }

        holder.thumbnail.bubbleStyle = when {
            !canGroupWithPrevious && canGroupWithNext -> if (message.isMe()) BubbleImageView.Style.OUT_FIRST else BubbleImageView.Style.IN_FIRST
            canGroupWithPrevious && canGroupWithNext -> if (message.isMe()) BubbleImageView.Style.OUT_MIDDLE else BubbleImageView.Style.IN_MIDDLE
            canGroupWithPrevious && !canGroupWithNext -> if (message.isMe()) BubbleImageView.Style.OUT_LAST else BubbleImageView.Style.IN_LAST
            else -> BubbleImageView.Style.ONLY
        }

        tryOrNull(true) {
            Glide.with(context).load(part.getUri()).fitCenter().into(holder.thumbnail)
        }
    }

}