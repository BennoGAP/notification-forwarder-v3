package org.groebl.sms.feature.settings.simconfigure

import org.groebl.sms.common.base.QkViewContract
import org.groebl.sms.common.widget.PreferenceView
import io.reactivex.Observable

interface SimConfigureView : QkViewContract<SimConfigureState> {

    enum class Action { SIM1, SIM2, SIM3 }

    fun preferenceClicks(): Observable<PreferenceView>
    fun actionClicks(): Observable<Action>
    fun actionSelected(): Observable<Int>

    fun showSimConfigure(selected: Int)

}