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
package org.groebl.sms.common.util

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

class CrashlyticsTree @Inject constructor(
    private val context: Context
) : Timber.Tree()
{

    override fun log(priority: Int, tag: String?, message: String?, throwable: Throwable?) {
        val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(sharedPrefs.getBoolean("optOut", true))

        val priorityString = when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            else -> "WTF"
        }

        crashlytics.log("$priorityString/$tag: $message")
        throwable?.run(crashlytics::recordException)
    }

}
