package org.groebl.sms.feature.blocking.regexps

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.jakewharton.rxbinding2.view.clicks
import org.groebl.sms.R
import org.groebl.sms.common.base.QkController
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.extensions.setBackgroundTint
import org.groebl.sms.common.util.extensions.setTint
import org.groebl.sms.injection.appComponent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.blocked_regexps_add_dialog.view.input
import kotlinx.android.synthetic.main.blocked_regexps_controller.*
import javax.inject.Inject

class BlockedRegexpsController : QkController<BlockedRegexpsView, BlockedRegexpsState, BlockedRegexpsPresenter>(),
        BlockedRegexpsView {

    @Inject override lateinit var presenter: BlockedRegexpsPresenter
    @Inject lateinit var colors: Colors

    private val adapter = BlockedRegexpsAdapter()
    private val saveRegexSubject: Subject<String> = PublishSubject.create()

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
        layoutRes = R.layout.blocked_regexps_controller
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.blocked_regexps_title)
        showBackButton(true)
    }

    override fun onViewCreated() {
        super.onViewCreated()
        add.setBackgroundTint(colors.theme().theme)
        add.setTint(colors.theme().textPrimary)
        bannerIcon.setTint(colors.theme().theme)
        adapter.emptyView = empty
        regexps.adapter = adapter
    }

    override fun render(state: BlockedRegexpsState) {
        adapter.updateData(state.regexps)
    }

    override fun unblockRegex(): Observable<Long> = adapter.unblockRegex
    override fun addRegex(): Observable<*> = add.clicks()
    override fun bannerRegexps(): Observable<*> = bannerRegexps.clicks()
    override fun saveRegex(): Observable<String> = saveRegexSubject

    override fun showAddDialog() {
        val layout = LayoutInflater.from(activity).inflate(R.layout.blocked_regexps_add_dialog, null)
        val dialog = AlertDialog.Builder(activity!!)
            .setView(layout)
            .setPositiveButton(R.string.blocked_regexps_dialog_block) { _, _ ->
                if (layout.input.text.toString().trim().isNotEmpty()) {
                    saveRegexSubject.onNext(layout.input.text.toString())
                }
            }
            .setNegativeButton(R.string.button_cancel) { _, _ -> }
        dialog.show()
    }
}
