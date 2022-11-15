package org.groebl.sms.feature.settings.simconfigure

import android.content.Context
import android.view.View
import org.groebl.sms.R
import org.groebl.sms.common.base.QkController
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.extensions.*
import org.groebl.sms.common.widget.PreferenceView
import org.groebl.sms.injection.appComponent
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.settings_controller.preferences
import kotlinx.android.synthetic.main.settings_switch_widget.view.*
import kotlinx.android.synthetic.main.sim_configure_controller.*
import org.groebl.sms.common.QkDialog
import javax.inject.Inject

class SimConfigureController : QkController<SimConfigureView, SimConfigureState, SimConfigurePresenter>(), SimConfigureView {

    @Inject override lateinit var presenter: SimConfigurePresenter
    @Inject lateinit var actionsDialog: QkDialog
    @Inject lateinit var colors: Colors
    @Inject lateinit var context: Context

    /**
     * Allows us to subscribe to [actionClicks] more than once
     */
    private val actionClicks: Subject<SimConfigureView.Action> = PublishSubject.create()

    init {
        appComponent.inject(this)
        layoutRes = R.layout.sim_configure_controller

        actionsDialog.adapter.setData(R.array.settings_sim_colors)
    }

    override fun onViewCreated() {

        /*sim1_holder.postDelayed({ sim1_holder?.animateLayoutChanges = true }, 100)
        sim2_holder.postDelayed({ sim2_holder?.animateLayoutChanges = true }, 100)
        sim3_holder.postDelayed({ sim3_holder?.animateLayoutChanges = true }, 100)*/

        Observable.merge(
            sim1_holder.clicks().map { SimConfigureView.Action.SIM1 },
            sim2_holder.clicks().map { SimConfigureView.Action.SIM2 },
            sim3_holder.clicks().map { SimConfigureView.Action.SIM3 })
                .autoDisposable(scope())
                .subscribe(actionClicks)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.settings_sim_configure_title)
        showBackButton(true)
    }

    override fun preferenceClicks(): Observable<PreferenceView> = (0 until preferences.childCount)
        .map { index -> preferences.getChildAt(index) }
        .mapNotNull { view -> view as? PreferenceView }
        .map { preference -> preference.clicks().map { preference } }
        .let { preferences -> Observable.merge(preferences) }

    override fun actionClicks(): Observable<SimConfigureView.Action> = actionClicks

    override fun actionSelected(): Observable<Int> = actionsDialog.adapter.menuItemClicks

    override fun showSimConfigure(selected: Int) {
        actionsDialog.adapter.selectedItem = selected
        activity?.let(actionsDialog::show)
    }

    override fun render(state: SimConfigureState) {
        simColor.checkbox.isChecked = state.simColor

        sim1_holder.isEnabled = state.simColor
        sim1_title.isEnabled = state.simColor
        sim1_change.isEnabled = state.simColor
        if (state.simColor) sim1_icon.setTint(state.sim1Color) else sim1_icon.setTint(colors.theme().textSecondary)

        sim2_holder.isEnabled = state.simColor
        sim2_title.isEnabled = state.simColor
        sim2_change.isEnabled = state.simColor
        if (state.simColor) sim2_icon.setTint(state.sim2Color) else sim2_icon.setTint(colors.theme().textSecondary)

        sim3_holder.isEnabled = state.simColor
        sim3_title.isEnabled = state.simColor
        sim3_change.isEnabled = state.simColor
        if (state.simColor) sim3_icon.setTint(state.sim3Color) else sim3_icon.setTint(colors.theme().textSecondary)

    }

}