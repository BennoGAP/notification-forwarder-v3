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

import org.groebl.sms.extensions.anyOf
import org.groebl.sms.model.BlockedNumber
import org.groebl.sms.model.BlockedRegex
import org.groebl.sms.util.PhoneNumberUtils
import io.realm.Realm
import io.realm.RealmResults
import javax.inject.Inject

class BlockingRepositoryImpl @Inject constructor(
    private val phoneNumberUtils: PhoneNumberUtils
) : BlockingRepository {

    override fun blockNumber(vararg addresses: String) {
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()

            val blockedNumbers = realm.where(BlockedNumber::class.java).findAll()
            val newAddresses = addresses.filter { address ->
                blockedNumbers.none { number -> phoneNumberUtils.compare(number.address, address) }
            }

            val maxId = realm.where(BlockedNumber::class.java)
                    .max("id")?.toLong() ?: -1

            realm.executeTransaction {
                realm.insert(newAddresses.mapIndexed { index, address ->
                    BlockedNumber(maxId + 1 + index, address)
                })
            }
        }
    }

    override fun blockRegex(vararg regexps: String) {
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()

            val blockedRegexps = realm.where(BlockedRegex::class.java).findAll()
            val newRegexps = regexps.filter { regex ->
                blockedRegexps.none { blockedRegex -> phoneNumberUtils.compare(blockedRegex.regex, regex) }
            }

            val maxId = realm.where(BlockedRegex::class.java)
                .max("id")?.toLong() ?: -1

            realm.executeTransaction {
                realm.insert(newRegexps.mapIndexed { index, regex ->
                    BlockedRegex(maxId + 1 + index, regex)
                })
            }
        }
    }

    override fun getBlockedNumbers(): RealmResults<BlockedNumber> {
        return Realm.getDefaultInstance()
                .where(BlockedNumber::class.java)
                .findAllAsync()
    }

    override fun getBlockedNumber(id: Long): BlockedNumber? {
        return Realm.getDefaultInstance()
                .where(BlockedNumber::class.java)
                .equalTo("id", id)
                .findFirst()
    }

    override fun getBlockedRegexps(): RealmResults<BlockedRegex> {
        return Realm.getDefaultInstance()
            .where(BlockedRegex::class.java)
            .findAllAsync()
    }

    override fun getBlockedRegex(id: Long): BlockedRegex? {
        return Realm.getDefaultInstance()
            .where(BlockedRegex::class.java)
            .equalTo("id", id)
            .findFirst()
    }

    override fun isBlocked(address: String): Boolean {
        return Realm.getDefaultInstance().use { realm ->
            realm.where(BlockedNumber::class.java)
                    .findAll()
                    .any { number -> phoneNumberUtils.compare(number.address, address) }
        }
    }

    override fun isBlockedContent(content: String): Boolean {
        val blockedRegexps = Realm.getDefaultInstance()
            .where(BlockedRegex::class.java)
            .findAll()
        for (blockedRegex in blockedRegexps){
            val regex = Regex(blockedRegex.regex)
            if (regex.containsMatchIn(content)) return true
        }
        return false
    }

    override fun unblockNumber(id: Long) {
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                realm.where(BlockedNumber::class.java)
                        .equalTo("id", id)
                        .findAll()
                        .deleteAllFromRealm()
            }
        }
    }

    override fun unblockNumbers(vararg addresses: String) {
        Realm.getDefaultInstance().use { realm ->
            val ids = realm.where(BlockedNumber::class.java)
                    .findAll()
                    .filter { number ->
                        addresses.any { address -> phoneNumberUtils.compare(number.address, address) }
                    }
                    .map { number -> number.id }
                    .toLongArray()

            realm.executeTransaction {
                realm.where(BlockedNumber::class.java)
                        .anyOf("id", ids)
                        .findAll()
                        .deleteAllFromRealm()
            }
        }
    }

    override fun unblockRegex(id: Long) {
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                realm.where(BlockedRegex::class.java)
                    .equalTo("id", id)
                    .findAll()
                    .deleteAllFromRealm()
            }
        }
    }

    override fun unblockRegexps(vararg regexps: String) {
        Realm.getDefaultInstance().use { realm ->
            val ids = realm.where(BlockedRegex::class.java)
                .findAll()
                .filter { blockedRegex ->
                    regexps.any { address -> phoneNumberUtils.compare(blockedRegex.regex, address) }
                }
                .map { number -> number.id }
                .toLongArray()

            realm.executeTransaction {
                realm.where(BlockedRegex::class.java)
                    .anyOf("id", ids)
                    .findAll()
                    .deleteAllFromRealm()
            }
        }
    }

}
