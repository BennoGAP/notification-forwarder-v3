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

package org.groebl.sms.common.util

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import org.groebl.sms.manager.AnalyticsManager
import org.groebl.sms.manager.BillingManager
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManagerImpl @Inject constructor(
        context: Context,
        private val analyticsManager: AnalyticsManager
) : BillingManager, BillingClientStateListener, PurchasesUpdatedListener, PurchasesResponseListener {

    private val productsSubject: Subject<List<SkuDetails>> = BehaviorSubject.create()
    override val products: Observable<List<BillingManager.Product>> = productsSubject
            .map { skuDetailsList ->
                skuDetailsList.map { skuDetails ->
                    BillingManager.Product(skuDetails.sku, skuDetails.price, skuDetails.priceCurrencyCode)
                }
            }

    private val purchaseListSubject = BehaviorSubject.create<List<Purchase>>()
    override val upgradeStatus: Observable<Boolean> = purchaseListSubject
            .map { purchases ->
                purchases
                        .any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            }
            .distinctUntilChanged()
            .doOnNext { upgraded -> analyticsManager.setUserProperty("Upgraded", upgraded) }

    private val skus = listOf(BillingManager.SKU_01, BillingManager.SKU_02, BillingManager.SKU_03, BillingManager.SKU_04)
    private val billingClient: BillingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

    private val billingClientState = MutableSharedFlow<Int>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        billingClientState.tryEmit(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
    }

    override suspend fun checkForPurchases() = executeServiceRequest {
        // Load the cached data
        queryPurchases()

        // On a fresh device, the purchase might not be cached, and so we'll need to force a refresh
        billingClient.queryPurchaseHistory(BillingClient.SkuType.INAPP)
        queryPurchases()
    }

    override suspend fun queryProducts() = executeServiceRequest {
        val params = SkuDetailsParams.newBuilder()
                .setSkusList(skus)
                .setType(BillingClient.SkuType.INAPP)

        val (billingResult, skuDetailsList) = billingClient.querySkuDetails(params.build())
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            productsSubject.onNext(skuDetailsList.orEmpty())
        }
    }

    override suspend fun initiatePurchaseFlow(activity: Activity, sku: String) = executeServiceRequest {
        val skuDetails = withContext(Dispatchers.IO) {
            val params = SkuDetailsParams.newBuilder()
                    .setType(BillingClient.SkuType.INAPP)
                    .setSkusList(listOf(sku))
                    .build()

            billingClient.querySkuDetails(params).skuDetailsList?.firstOrNull()!!
        }

        val params = BillingFlowParams.newBuilder().setSkuDetails(skuDetails)
        billingClient.launchBillingFlow(activity, params.build())
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            GlobalScope.launch(Dispatchers.IO) {
                handlePurchases(purchases.orEmpty())
            }
        }
    }

    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, this)
        //billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, this)
    }

    override fun onQueryPurchasesResponse(result: BillingResult, purchases: MutableList<Purchase>) {
        if(result.responseCode == BillingClient.BillingResponseCode.OK) {
            GlobalScope.launch(Dispatchers.IO) {
                handlePurchases(purchases)
            }
        }
        /*
        Consume Purchases (ONLY FOR DEBUG)
        purchases.forEach { purchase ->
            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.consumeAsync(params) { responseCode, purchaseToken ->
                when (responseCode.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        Timber.i("Used OK token $purchaseToken")
                    }
                    else -> {
                        Timber.i("Used Other token $purchaseToken")
                    }
                }
            }
        }
        */
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) = executeServiceRequest {
        purchases.forEach { purchase ->
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

                Timber.i("Acknowledging purchase ${purchase.orderId}")
                val result = billingClient.acknowledgePurchase(params)
                Timber.i("Acknowledgement result: ${result.responseCode}, ${result.debugMessage}")
            }
        }

        purchaseListSubject.onNext(purchases)
    }

    private suspend fun executeServiceRequest(runnable: suspend () -> Unit) {
        if (billingClientState.first() != BillingClient.BillingResponseCode.OK) {
            Timber.i("Starting billing service")
            billingClient.startConnection(this)
        }

        billingClientState.first { state -> state == BillingClient.BillingResponseCode.OK }
        runnable()
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        Timber.i("Billing response: ${result.responseCode}")
        billingClientState.tryEmit(result.responseCode)
    }

    override fun onBillingServiceDisconnected() {
        Timber.i("Billing service disconnected")
        billingClientState.tryEmit(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
    }

}