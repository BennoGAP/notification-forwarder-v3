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

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClient.SkuType
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.groebl.sms.manager.AnalyticsManager
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
        context: Context,
        private val analyticsManager: AnalyticsManager
) : PurchasesUpdatedListener {

    companion object {
        const val SKU_01 = "donate01" // 1,50 Euro
        const val SKU_02 = "donate02" // 3 Euro
        const val SKU_03 = "donate03" // 5 Euro
        const val SKU_04 = "donate04" // 8 Euro
    }

    val products: Observable<List<SkuDetails>> = BehaviorSubject.create()
    val upgradeStatus: Observable<Boolean>

    private val skus = listOf(SKU_01, SKU_02, SKU_03, SKU_04)
    private val purchaseListObservable = BehaviorSubject.create<List<Purchase>>()

    private val billingClient: BillingClient = BillingClient.newBuilder(context).setListener(this).build()
    private var isServiceConnected = false

    init {
        startServiceConnection {
            queryPurchases()
            querySkuDetailsAsync()
        }

        upgradeStatus = purchaseListObservable
                .map { purchases ->
                    purchases.any { it.sku == SKU_01 } ||
                    purchases.any { it.sku == SKU_02 } ||
                    purchases.any { it.sku == SKU_03 } ||
                    purchases.any { it.sku == SKU_04 } }
                .doOnNext {
                    upgraded -> analyticsManager.setUserProperty("Upgraded", upgraded)
                }
    }

    private fun queryPurchases() {
        executeServiceRequest {
            // Load the cached data
            purchaseListObservable.onNext(billingClient.queryPurchases(SkuType.INAPP).purchasesList.orEmpty())

            // On a fresh device, the purchase might not be cached, and so we'll need to force a refresh
            billingClient.queryPurchaseHistoryAsync(SkuType.INAPP) { _, _ ->
                purchaseListObservable.onNext(billingClient.queryPurchases(SkuType.INAPP).purchasesList.orEmpty())
            }
        }
    }


    private fun startServiceConnection(onSuccess: () -> Unit) {
        val listener = object : BillingClientStateListener {
            override fun onBillingSetupFinished(@BillingResponse billingResponseCode: Int) {
                if (billingResponseCode == BillingResponse.OK) {
                    isServiceConnected = true
                    onSuccess()
                } else {
                    Timber.w("Billing response: $billingResponseCode")
                    purchaseListObservable.onNext(listOf())
                }
            }

            override fun onBillingServiceDisconnected() {
                isServiceConnected = false
            }
        }

        Flowable.fromCallable { billingClient.startConnection(listener) }
        .subscribeOn(Schedulers.io())
        .subscribe()
    }

    private fun querySkuDetailsAsync() {
        executeServiceRequest {
            val subParams = SkuDetailsParams.newBuilder().setSkusList(skus).setType(BillingClient.SkuType.INAPP)
            billingClient.querySkuDetailsAsync(subParams.build()) { responseCode, skuDetailsList ->
                if (responseCode == BillingResponse.OK) {
                    (products as Subject).onNext(skuDetailsList)
                }
            }
        }
    }

    fun initiatePurchaseFlow(activity: Activity, sku: String) {
        executeServiceRequest {
            val params = BillingFlowParams.newBuilder().setSku(sku).setType(SkuType.INAPP)
            billingClient.launchBillingFlow(activity, params.build())
        }
    }

    private fun executeServiceRequest(runnable: () -> Unit) {
        when (isServiceConnected) {
            true -> runnable()
            false -> startServiceConnection(runnable)
        }
    }

    override fun onPurchasesUpdated(resultCode: Int, purchases: List<Purchase>?) {
        if (resultCode == BillingResponse.OK) {
            purchaseListObservable.onNext(purchases.orEmpty())
        }
    }

}