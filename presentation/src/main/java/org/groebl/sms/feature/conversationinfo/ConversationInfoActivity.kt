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

import android.os.Build
import android.os.Bundle
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import org.groebl.sms.R
import org.groebl.sms.common.base.QkThemedActivity
import org.groebl.sms.common.util.extensions.getColorCompat
import org.groebl.sms.common.util.extensions.resolveThemeColor
import org.groebl.sms.util.Preferences
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.container_activity.*

class ConversationInfoActivity : QkThemedActivity() {

    private lateinit var router: Router

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.container_activity)
        toolbar.navigationIcon?.setTint(resolveThemeColor(android.R.attr.textColorSecondary))

        if (!isNightMode()) {
            val backgroundGray = getColorCompat(R.color.backgroundGray)
            toolbar.setBackgroundColor(backgroundGray)
            window.navigationBarColor = backgroundGray
            window.statusBarColor = backgroundGray
        }

        router = Conductor.attachRouter(this, container, savedInstanceState)
        if (!router.hasRootController()) {
            val threadId = intent.extras?.getLong("threadId") ?: 0L
            router.setRoot(RouterTransaction.with(ConversationInfoController(threadId)))
        }
    }

    private fun isNightMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (prefs.nightMode.get() == Preferences.NIGHT_MODE_SYSTEM) resources.configuration.isNightModeActive
            else prefs.night.get()
        } else {
            prefs.night.get()
        }
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }

}