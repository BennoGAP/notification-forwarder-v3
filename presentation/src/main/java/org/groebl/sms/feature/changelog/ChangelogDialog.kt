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
package org.groebl.sms.feature.changelog

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import org.groebl.sms.BuildConfig
import org.groebl.sms.R
import org.groebl.sms.feature.main.MainActivity
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.changelog_dialog.view.*

class ChangelogDialog(activity: MainActivity) {

    private val dialog: AlertDialog
    private val adapter = ChangelogAdapter(
            activity.resources.getStringArray(R.array.changelog_keywords).asList(),
            activity.resources.getStringArray(R.array.changelog).asList())

    init {
        val layout = LayoutInflater.from(activity).inflate(R.layout.changelog_dialog, null)

        dialog = AlertDialog.Builder(activity)
                .setCancelable(true)
                .setView(layout)
                .create()

        layout.version.text = activity.getString(R.string.changelog_version, BuildConfig.VERSION_NAME)
        layout.changelog.adapter = adapter
        layout.dismiss.setOnClickListener { dialog.dismiss() }
    }

    fun show() = dialog.show()

}
