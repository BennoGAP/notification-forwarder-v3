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
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import org.groebl.sms.R
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.extensions.resolveThemeColor
import org.groebl.sms.common.util.extensions.setBackgroundTint
import org.groebl.sms.common.util.extensions.setTint
import org.groebl.sms.extensions.isVCard
import org.groebl.sms.extensions.mapNotNull
import org.groebl.sms.feature.compose.BubbleUtils
import org.groebl.sms.model.Message
import org.groebl.sms.model.MmsPart
import ezvcard.Ezvcard
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.mms_vcard_list_item.view.*
import javax.inject.Inject

class VCardBinder @Inject constructor(colors: Colors, private val context: Context) : PartBinder() {

    override val partLayout = R.layout.mms_vcard_list_item
    override var theme = colors.theme()

    override fun canBindPart(part: MmsPart) = part.isVCard()

    override fun bindPart(
        view: View,
        part: MmsPart,
        message: Message,
        canGroupWithPrevious: Boolean,
        canGroupWithNext: Boolean
    ) {
        BubbleUtils.getBubble(false, canGroupWithPrevious, canGroupWithNext, message.isMe())
                .let(view.vCardBackground::setBackgroundResource)

        view.setOnClickListener { clicks.onNext(part.id) }

        Observable.just(part.getUri())
                .map(context.contentResolver::openInputStream)
                .mapNotNull { inputStream -> inputStream.use { Ezvcard.parse(it).first() } }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { vcard -> view.name?.text = vcard.formattedName.value }

        val params = view.vCardBackground.layoutParams as FrameLayout.LayoutParams
        if (!message.isMe()) {
            view.vCardBackground.layoutParams = params.apply { gravity = Gravity.START }
            view.vCardBackground.setBackgroundTint(theme.theme)
            view.vCardAvatar.setTint(theme.textPrimary)
            view.name.setTextColor(theme.textPrimary)
            view.label.setTextColor(theme.textTertiary)
        } else {
            view.vCardBackground.layoutParams = params.apply { gravity = Gravity.END }
            view.vCardBackground.setBackgroundTint(view.context.resolveThemeColor(R.attr.bubbleColor))
            view.vCardAvatar.setTint(view.context.resolveThemeColor(android.R.attr.textColorSecondary))
            view.name.setTextColor(view.context.resolveThemeColor(android.R.attr.textColorPrimary))
            view.label.setTextColor(view.context.resolveThemeColor(android.R.attr.textColorTertiary))
        }
    }

}