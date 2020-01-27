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
package org.groebl.sms.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import kotlinx.android.synthetic.main.group_avatar_view.view.*
import org.groebl.sms.R
import org.groebl.sms.common.util.extensions.getColorCompat
import org.groebl.sms.common.util.extensions.resolveThemeColor
import org.groebl.sms.common.util.extensions.setBackgroundTint
import org.groebl.sms.model.Recipient

class GroupAvatarView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    var contacts: List<Recipient> = ArrayList()
        set(value) {
            field = value
            updateView()
        }

    init {
        View.inflate(context, R.layout.group_avatar_view, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (!isInEditMode) {
            updateView()
        }
    }

    private fun updateView() {
        avatar1Frame.setBackgroundTint(when (contacts.size > 1) {
            true -> context.resolveThemeColor(android.R.attr.windowBackground)
            false -> context.getColorCompat(android.R.color.transparent)
        })
        avatar1Frame.updateLayoutParams<LayoutParams> {
            matchConstraintPercentWidth = if (contacts.size > 1) 0.75f else 1.0f
        }
        avatar2.isVisible = contacts.size > 1

        avatar1.setContact(contacts.getOrNull(0))
        avatar2.setContact(contacts.getOrNull(1))
    }

}