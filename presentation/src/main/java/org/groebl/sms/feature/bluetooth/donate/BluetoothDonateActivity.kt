package org.groebl.sms.feature.bluetooth.donate

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.jakewharton.rxbinding2.view.clicks
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.bluetooth_donate_activity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.groebl.sms.R
import org.groebl.sms.common.base.QkThemedActivity
import org.groebl.sms.common.util.extensions.makeToast
import org.groebl.sms.manager.BillingManager
import org.groebl.sms.common.util.extensions.setVisible
import timber.log.Timber
import javax.inject.Inject

class BluetoothDonateActivity : QkThemedActivity(), BluetoothDonateView {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by lazy { ViewModelProvider(this, viewModelFactory)[BluetoothDonateViewModel::class.java] }

    override val donateIntent1 by lazy { donate1.clicks() }
    override val donateIntent2 by lazy { donate2.clicks() }
    override val donateIntent3 by lazy { donate3.clicks() }
    override val donateIntent4 by lazy { donate4.clicks() }
    override val donateIntentPaypal by lazy { donate_paypal.clicks() }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth_donate_activity)
        setTitle(R.string.settings_bluetooth_donate)
        showBackButton(true)
        viewModel.bindView(this)
    }

    override fun render(state: BluetoothDonateState) {

        layout_donate_thanks.setVisible(state.upgraded)

        if(state.upgradePrice1 == "" || state.upgradePrice2 == "" || state.upgradePrice3 == "" || state.upgradePrice4 == "") {
            layout_donate_google.setVisible(false)
            layout_donate_paypal.setVisible(true)
        } else {
            donate1.title = getString(R.string.bluetooth_donate_price, state.upgradePrice1)
            donate2.title = getString(R.string.bluetooth_donate_price, state.upgradePrice2)
            donate3.title = getString(R.string.bluetooth_donate_price, state.upgradePrice3)
            donate4.title = getString(R.string.bluetooth_donate_price, state.upgradePrice4)
            layout_donate_google.setVisible(true)
            layout_donate_paypal.setVisible(false)
        }
    }

    override fun initiatePurchaseFlow(billingManager: BillingManager, sku: String) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                billingManager.initiatePurchaseFlow(this@BluetoothDonateActivity, sku)
            } catch (e: Exception) {
                Timber.w(e)
                makeToast(R.string.qksms_plus_error)
            }
        }
    }

}