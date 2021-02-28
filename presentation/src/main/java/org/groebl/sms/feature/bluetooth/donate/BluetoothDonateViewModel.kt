package org.groebl.sms.feature.bluetooth.donate

import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import org.groebl.sms.common.Navigator
import org.groebl.sms.common.base.QkViewModel
import org.groebl.sms.manager.BillingManager
import org.groebl.sms.manager.AnalyticsManager
import javax.inject.Inject

class BluetoothDonateViewModel @Inject constructor(
        private val analyticsManager: AnalyticsManager,
        private val billingManager: BillingManager,
        private val navigator: Navigator
) : QkViewModel<BluetoothDonateView, BluetoothDonateState>(BluetoothDonateState()) {

    init {
        disposables += billingManager.upgradeStatus
                .subscribe { upgraded -> newState { copy(upgraded = upgraded) } }

        disposables += billingManager.products
                .subscribe { products ->
                    newState {
                        val upgrade1 = products.firstOrNull { it.sku == BillingManager.SKU_01 }
                        val upgrade2 = products.firstOrNull { it.sku == BillingManager.SKU_02 }
                        val upgrade3 = products.firstOrNull { it.sku == BillingManager.SKU_03 }
                        val upgrade4 = products.firstOrNull { it.sku == BillingManager.SKU_04 }
                        copy(
                                upgradePrice1 = upgrade1?.price ?: "", upgradePrice2 = upgrade2?.price ?: "",
                                upgradePrice3 = upgrade3?.price ?: "", upgradePrice4 = upgrade4?.price ?: "")
                        //currency = upgrade1?.priceCurrencyCode ?: upgrade2?.priceCurrencyCode ?: upgrade3?.priceCurrencyCode ?: upgrade4?.priceCurrencyCode ?: "")
                    }
                }
    }

    override fun bindView(view: BluetoothDonateView) {
        super.bindView(view)

        Observable.merge(
                view.donateIntent1.map { BillingManager.SKU_01 },
                view.donateIntent2.map { BillingManager.SKU_02 },
                view.donateIntent3.map { BillingManager.SKU_03 },
                view.donateIntent4.map { BillingManager.SKU_04 })
                .doOnNext { sku -> analyticsManager.track("Clicked Upgrade", Pair("sku", sku)) }
                .autoDisposable(view.scope())
                .subscribe { sku -> view.initiatePurchaseFlow(billingManager, sku) }

        view.donateIntentPaypal
                .autoDisposable(view.scope())
                .subscribe { navigator.showDonationBluetooth() }
    }

}