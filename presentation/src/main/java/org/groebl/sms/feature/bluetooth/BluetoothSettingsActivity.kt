package org.groebl.sms.feature.bluetooth

import android.os.Bundle
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import org.groebl.sms.R
import org.groebl.sms.common.base.QkThemedActivity
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.container_activity.*
import org.groebl.sms.common.util.extensions.resolveThemeColor

class BluetoothSettingsActivity : QkThemedActivity() {

    private lateinit var router: Router

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.container_activity)

        toolbar.navigationIcon?.setTint(resolveThemeColor(android.R.attr.textColorSecondary))

        router = Conductor.attachRouter(this, container, savedInstanceState)
        if (!router.hasRootController()) {
            router.setRoot(RouterTransaction.with(BluetoothSettingsController()))
        }
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }

}