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
package org.groebl.sms.manager

import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManagerImpl @Inject constructor() : AnalyticsManager {

    override fun track(event: String, vararg properties: Pair<String, Any>) {

        // Log the event, but don't do anything else
        JSONObject(properties
            .associateBy { pair -> pair.first }
            .mapValues { pair -> pair.value.second })
            .also { Timber.v("$event: $it") }
    }

    override fun setUserProperty(key: String, value: Any) {
        Timber.v("$key: $value")
    }

}
