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
package org.groebl.sms.feature.compose.part

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import org.groebl.sms.R
import org.groebl.sms.common.base.QkViewHolder
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.extensions.resolveThemeColor
import org.groebl.sms.common.util.extensions.setBackgroundTint
import org.groebl.sms.common.util.extensions.setTint
import org.groebl.sms.feature.compose.BubbleUtils
import org.groebl.sms.model.Message
import org.groebl.sms.model.MmsPart
import org.groebl.sms.util.Preferences
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.mms_file_list_item.*
import javax.inject.Inject

class FileBinder @Inject constructor(colors: Colors, private val context: Context, private val prefs: Preferences) : PartBinder() {

    override val partLayout = R.layout.mms_file_list_item
    override var theme = colors.theme()

    // This is the last binder we check. If we're here, we can bind the part
    override fun canBindPart(part: MmsPart) = true

    @SuppressLint("CheckResult")
    override fun bindPart(
        holder: QkViewHolder,
        part: MmsPart,
        message: Message,
        canGroupWithPrevious: Boolean,
        canGroupWithNext: Boolean
    ) {
        BubbleUtils.getBubble(false, canGroupWithPrevious, canGroupWithNext, message.isMe(), style = prefs.bubbleStyle.get())
                .let(holder.fileBackground::setBackgroundResource)

        holder.containerView.setOnClickListener { clicks.onNext(part.id) }

        Observable.just(part.getUri())
                .map(context.contentResolver::openInputStream)
                .map { inputStream -> inputStream.use { it.available() } }
                .map { bytes ->
                    when (bytes) {
                        in 0..999 -> "$bytes B"
                        in 1000..999999 -> "${"%.1f".format(bytes / 1000f)} KB"
                        in 1000000..9999999 -> "${"%.1f".format(bytes / 1000000f)} MB"
                        else -> "${"%.1f".format(bytes / 1000000000f)} GB"
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { size -> holder.size.text = size }

        holder.filename.text = part.name

        val params = holder.fileBackground.layoutParams as FrameLayout.LayoutParams
        if (!message.isMe()) {
            holder.fileBackground.layoutParams = params.apply { gravity = Gravity.START }
            holder.fileBackground.setBackgroundTint(theme.theme)
            holder.icon.setTint(theme.textPrimary)
            holder.filename.setTextColor(theme.textPrimary)
            holder.size.setTextColor(theme.textTertiary)
        } else {
            holder.fileBackground.layoutParams = params.apply { gravity = Gravity.END }
            holder.fileBackground.setBackgroundTint(holder.containerView.context.resolveThemeColor(R.attr.bubbleColor))
            holder.icon.setTint(holder.containerView.context.resolveThemeColor(android.R.attr.textColorSecondary))
            holder.filename.setTextColor(holder.containerView.context.resolveThemeColor(android.R.attr.textColorPrimary))
            holder.size.setTextColor(holder.containerView.context.resolveThemeColor(android.R.attr.textColorTertiary))
        }
    }

}