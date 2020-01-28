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
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.RelativeLayout
import org.groebl.sms.R
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.extensions.setBackgroundTint
import org.groebl.sms.common.util.extensions.setTint
import org.groebl.sms.injection.appComponent
import org.groebl.sms.model.Recipient
import kotlinx.android.synthetic.main.contact_chip_detailed.view.*
import javax.inject.Inject

class DetailedChipView(context: Context) : RelativeLayout(context) {

    @Inject lateinit var colors: Colors

    init {
        View.inflate(context, R.layout.contact_chip_detailed, this)
        appComponent.inject(this)

        setOnClickListener { hide() }

        visibility = View.GONE

        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun setRecipient(recipient: Recipient) {
        avatar.setRecipient(recipient)
        name.text = recipient.contact?.name?.takeIf { it.isNotBlank() } ?: recipient.address
        info.text = recipient.address

        colors.theme(recipient).let { theme ->
            card.setBackgroundTint(theme.theme)
            name.setTextColor(theme.textPrimary)
            info.setTextColor(theme.textTertiary)
            delete.setTint(theme.textPrimary)
        }
    }

    fun show() {
        startAnimation(AlphaAnimation(0f, 1f).apply { duration = 200 })

        visibility = View.VISIBLE
        requestFocus()
        isClickable = true
    }

    fun hide() {
        startAnimation(AlphaAnimation(1f, 0f).apply { duration = 200 })

        visibility = View.GONE
        clearFocus()
        isClickable = false
    }

    fun setOnDeleteListener(listener: (View) -> Unit) {
        delete.setOnClickListener(listener)
    }

}
