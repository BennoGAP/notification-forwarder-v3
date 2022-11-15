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

import org.groebl.sms.repository.BlockingRepository
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

class QksmsBlockingClient @Inject constructor(
    private val blockingRepo: BlockingRepository
) : BlockingClient {

    override fun isAvailable(): Boolean = true

    override fun getClientCapability() = BlockingClient.Capability.BLOCK_WITHOUT_PERMISSION

    override fun shouldBlock(address: String): Single<BlockingClient.Action> = isBlacklisted(address)

    override fun isBlacklisted(address: String): Single<BlockingClient.Action> = Single.fromCallable {
        when (blockingRepo.isBlockedAddress(address)) {
            true -> BlockingClient.Action.Block()
            false -> BlockingClient.Action.Unblock
        }
    }

    override fun getActionFromContent(content: String): Single<BlockingClient.Action> = Single.fromCallable {
        when (blockingRepo.isBlockedContent(content)) {
            true -> BlockingClient.Action.Block("Blocked for content")
            false -> BlockingClient.Action.Unblock
        }
    }

    override fun blockAddresses(addresses: List<String>): Completable = Completable.fromCallable {
        blockingRepo.blockNumber(*addresses.toTypedArray())
    }

    override fun unblockAddresses(addresses: List<String>): Completable = Completable.fromCallable {
        blockingRepo.unblockNumbers(*addresses.toTypedArray())
    }

    override fun blockRegexps(regexps: List<String>): Completable = Completable.fromCallable  {
        blockingRepo.blockRegex(*regexps.toTypedArray())
    }

    override fun unblockRegexps(regexps: List<String>): Completable = Completable.fromCallable  {
        blockingRepo.unblockRegexps(*regexps.toTypedArray())
    }

    override fun openSettings() = Unit // TODO: Do this here once we implement AndroidX navigation

}
