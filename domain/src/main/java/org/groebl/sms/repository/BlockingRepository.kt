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
package org.groebl.sms.repository

import org.groebl.sms.model.BlockedNumber
import org.groebl.sms.model.BlockedRegex
import io.realm.RealmResults

interface BlockingRepository {

    fun blockNumber(vararg addresses: String)

    fun blockRegex(vararg regexps: String)

    fun getBlockedNumbers(): RealmResults<BlockedNumber>

    fun getBlockedNumber(id: Long): BlockedNumber?

    fun getBlockedRegexps(): RealmResults<BlockedRegex>

    fun getBlockedRegex(id: Long): BlockedRegex?

    fun isBlocked(address: String): Boolean

    fun isBlockedContent(content: String): Boolean

    fun unblockNumber(id: Long)

    fun unblockNumbers(vararg addresses: String)

    fun unblockRegex(id: Long)

    fun unblockRegexps(vararg regexps: String)

}
