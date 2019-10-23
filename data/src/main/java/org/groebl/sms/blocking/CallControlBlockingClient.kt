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
package org.groebl.sms.blocking

import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.core.database.getStringOrNull
import com.callcontrol.datashare.CallControl
import io.reactivex.Single
import org.groebl.sms.extensions.map
import org.groebl.sms.util.tryOrNull
import javax.inject.Inject

class CallControlBlockingClient @Inject constructor(
    private val context: Context
) : BlockingClient {

    private val projection: Array<String> = arrayOf(
            //CallControl.Lookup.DISPLAY_NAME, // This has a performance impact on the lookup, and we don't need it
            CallControl.Lookup.BLOCK_REASON
    )

    class LookupResult(cursor: Cursor) {
        val blockReason: String? = cursor.getStringOrNull(0)
    }

    override fun shouldBlock(address: String): Single<Boolean> {
        val uri = Uri.withAppendedPath(CallControl.LOOKUP_TEXT_URI, address)
        return Single.fromCallable {
            tryOrNull {
                context.contentResolver.query(uri, projection, null, null, null) // Query URI
                        ?.use { cursor -> cursor.map(::LookupResult) } // Map to Result object
                        ?.any { result -> result.blockReason != null } // Check if any are blocked
            } == true // If none are blocked or we errored at some point, return false
        }
    }

}