/*
 * Copyright (C) 2020 Moez Bhatti <moez.bhatti@gmail.com>
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

import android.app.Activity
import io.reactivex.Observable

interface BillingManager {

    companion object {
        const val SKU_01 = "donate01" // 1,50 Euro
        const val SKU_02 = "donate02" // 3 Euro
        const val SKU_03 = "donate03" // 5 Euro
        const val SKU_04 = "donate04" // 8 Euro
    }

    data class Product(
        val sku: String,
        val price: String,
        val priceCurrencyCode: String
    )

    val products: Observable<List<Product>>
    val upgradeStatus: Observable<Boolean>

    suspend fun checkForPurchases()

    suspend fun queryProducts()

    suspend fun initiatePurchaseFlow(activity: Activity, sku: String)

}
