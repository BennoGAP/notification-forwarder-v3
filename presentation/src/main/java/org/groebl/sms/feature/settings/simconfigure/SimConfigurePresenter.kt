package org.groebl.sms.feature.settings.simconfigure

import android.content.Context
import androidx.annotation.ColorRes
import org.groebl.sms.R
import org.groebl.sms.common.Navigator
import org.groebl.sms.common.base.QkPresenter
import org.groebl.sms.common.util.extensions.getColorCompat
import org.groebl.sms.manager.WidgetManager
import org.groebl.sms.util.Preferences
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.withLatestFrom
import javax.inject.Inject

class SimConfigurePresenter @Inject constructor(
    //context: Context,
    private val context: Context,
    private val navigator: Navigator,
    private val prefs: Preferences,
    private val widgetManager: WidgetManager
) : QkPresenter<SimConfigureView, SimConfigureState>(SimConfigureState()) {

    init {
        val actionLabels = context.resources.getStringArray(R.array.settings_sim_colors)

        disposables += prefs.simColor.asObservable()
            .subscribe { simColor -> newState { copy(simColor = simColor) }
                widgetManager.updateTheme()}

        disposables += prefs.sim1Color.asObservable()
            .subscribe { action -> newState { copy(sim1Label = actionLabels[action], sim1Color = colorForAction(context,action)) } }

        disposables += prefs.sim2Color.asObservable()
            .subscribe { action -> newState { copy(sim2Label = actionLabels[action], sim2Color = colorForAction(context,action)) } }

        disposables += prefs.sim3Color.asObservable()
            .subscribe { action -> newState { copy(sim3Label = actionLabels[action], sim3Color = colorForAction(context, action)) } }

    }

    override fun bindIntents(view: SimConfigureView) {
        super.bindIntents(view)

        view.preferenceClicks()
            .autoDisposable(view.scope())
            .subscribe { preference ->
                when (preference.id) {
                    R.id.simColor -> prefs.simColor.set(!prefs.simColor.get())
                }
            }

        view.actionClicks()
                .map { action ->
                    when (action) {
                        SimConfigureView.Action.SIM1 -> prefs.sim1Color.get()
                        SimConfigureView.Action.SIM2 -> prefs.sim2Color.get()
                        SimConfigureView.Action.SIM3 -> prefs.sim3Color.get()
                    }
                }
                .autoDisposable(view.scope())
                .subscribe(view::showSimConfigure)

        view.actionSelected()
                .withLatestFrom(view.actionClicks()) { actionId, action ->
                    when (action) {
                        SimConfigureView.Action.SIM1 -> prefs.sim1Color.set(actionId)
                        SimConfigureView.Action.SIM2 -> prefs.sim2Color.set(actionId)
                        SimConfigureView.Action.SIM3 -> prefs.sim3Color.set(actionId)
                    }
                }
                .autoDisposable(view.scope())
                .subscribe()

    }

    @ColorRes
    private fun colorForAction(context: Context, action: Int) = when (action) {
        Preferences.SIM_COLOR_BLUE -> context.getColorCompat(R.color.sim1)
        Preferences.SIM_COLOR_GREEN -> context.getColorCompat(R.color.sim2)
        Preferences.SIM_COLOR_YELLOW -> context.getColorCompat(R.color.sim3)
        Preferences.SIM_COLOR_RED -> context.getColorCompat(R.color.sim4)
        Preferences.SIM_COLOR_PURPLE -> context.getColorCompat(R.color.sim_other)
        else -> context.getColorCompat(R.color.sim1)
    }

}