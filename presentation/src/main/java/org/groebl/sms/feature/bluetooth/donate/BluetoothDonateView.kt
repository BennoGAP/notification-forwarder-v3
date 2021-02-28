package org.groebl.sms.feature.bluetooth.donate

import io.reactivex.Observable
import org.groebl.sms.common.base.QkView
import org.groebl.sms.manager.BillingManager

interface BluetoothDonateView : QkView<BluetoothDonateState> {

    val donateIntent1: Observable<Unit>
    val donateIntent2: Observable<Unit>
    val donateIntent3: Observable<Unit>
    val donateIntent4: Observable<Unit>
    val donateIntentPaypal: Observable<*>

    fun initiatePurchaseFlow(billingManager: BillingManager, sku: String)

}