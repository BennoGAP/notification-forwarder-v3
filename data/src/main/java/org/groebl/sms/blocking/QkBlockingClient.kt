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
import android.content.Intent
import android.os.Build
import android.provider.BlockedNumberContract
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import io.reactivex.Completable
import io.reactivex.Single
import io.realm.Realm
import org.groebl.sms.extensions.anyOf
import org.groebl.sms.model.BlockedNumber
import javax.inject.Inject

class QkBlockingClient @Inject constructor(private val context: Context) : BlockingClient {

    override fun isBlocked(address: String): Single<Boolean> = Single.fromCallable {
        when {
            Build.VERSION.SDK_INT >= 24 -> BlockedNumberContract.isBlocked(context, address)

            else -> Realm.getDefaultInstance()
                    .where(BlockedNumber::class.java)
                    .findAll()
                    .any { number -> PhoneNumberUtils.compare(number.address, address) }
        }
    }

    override fun canBlock(): Boolean = Build.VERSION.SDK_INT < 24

    override fun block(addresses: List<String>): Completable = Completable.fromCallable {
        when {
            Build.VERSION.SDK_INT >= 24 -> {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                val intent = telecomManager.createManageBlockedNumbersIntent()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }

            else -> Realm.getDefaultInstance().use { realm ->
                val maxId = realm.where(BlockedNumber::class.java)
                        .max("id")?.toLong() ?: -1

                realm.executeTransaction {
                    realm.insert(addresses.mapIndexed { index, address -> BlockedNumber(maxId + 1 + index, address) })
                }
            }
        }
    }

    override fun canUnblock(): Boolean = true

    override fun unblock(addresses: List<String>): Completable = Completable.fromCallable {
        when {
            Build.VERSION.SDK_INT >= 24 -> addresses.forEach { address ->
                BlockedNumberContract.unblock(context, address)
            }

            else -> Realm.getDefaultInstance().use { realm ->
                val ids = realm
                        .where(BlockedNumber::class.java)
                        .findAll()
                        .filter { number ->
                            addresses.any { address -> PhoneNumberUtils.compare(number.address, address) }
                        }
                        .map { number -> number.id }
                        .toLongArray()

                realm.executeTransaction {
                    realm.where(BlockedNumber::class.java)
                            .anyOf("id", ids)
                }
            }
        }
        }
}
