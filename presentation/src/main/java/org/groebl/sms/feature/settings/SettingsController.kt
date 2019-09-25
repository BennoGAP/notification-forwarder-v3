package org.groebl.sms.feature.settings

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import android.view.View
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.RouterTransaction
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.longClicks
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.settings_controller.*
import kotlinx.android.synthetic.main.settings_switch_widget.view.*
import kotlinx.android.synthetic.main.settings_theme_widget.*
import org.groebl.sms.R
import org.groebl.sms.common.QkChangeHandler
import org.groebl.sms.common.QkDialog
import org.groebl.sms.common.base.QkController
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.extensions.animateLayoutChanges
import org.groebl.sms.common.util.extensions.setBackgroundTint
import org.groebl.sms.common.util.extensions.setVisible
import org.groebl.sms.common.widget.FieldDialog
import org.groebl.sms.common.widget.PreferenceView
import org.groebl.sms.feature.settings.about.AboutController
import org.groebl.sms.feature.settings.swipe.SwipeActionsController
import org.groebl.sms.feature.themepicker.ThemePickerController
import org.groebl.sms.injection.appComponent
import org.groebl.sms.repository.SyncRepository
import org.groebl.sms.util.Preferences
import javax.inject.Inject

class SettingsController : QkController<SettingsView, SettingsState, SettingsPresenter>(), SettingsView {

    @Inject lateinit var context: Context
    @Inject lateinit var colors: Colors
    @Inject lateinit var nightModeDialog: QkDialog
    @Inject lateinit var textSizeDialog: QkDialog
    @Inject lateinit var sendDelayDialog: QkDialog
    @Inject lateinit var mmsSizeDialog: QkDialog

    @Inject override lateinit var presenter: SettingsPresenter

    private val signatureDialog: FieldDialog by lazy {
        FieldDialog(activity!!, context.getString(R.string.settings_signature_title), signatureSubject::onNext)
    }

    private val startTimeSelectedSubject: Subject<Pair<Int, Int>> = PublishSubject.create()
    private val endTimeSelectedSubject: Subject<Pair<Int, Int>> = PublishSubject.create()
    private val signatureSubject: Subject<String> = PublishSubject.create()

    private val progressAnimator by lazy { ObjectAnimator.ofInt(syncingProgress, "progress", 0, 0) }

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
        layoutRes = R.layout.settings_controller

        colors.themeObservable()
                .autoDisposable(scope())
                .subscribe { activity?.recreate() }
    }

    override fun onViewCreated() {
        preferences.postDelayed({ preferences?.animateLayoutChanges = true }, 100)

        nightModeDialog.adapter.setData(R.array.night_modes)
        textSizeDialog.adapter.setData(R.array.text_sizes)
        sendDelayDialog.adapter.setData(R.array.delayed_sending_labels)
        mmsSizeDialog.adapter.setData(R.array.mms_sizes, R.array.mms_sizes_ids)

        //about.summary = context.getString(R.string.settings_version, BuildConfig.VERSION_NAME)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.title_settings)
        showBackButton(true)
    }

    override fun preferenceClicks(): Observable<PreferenceView> = (0 until preferences.childCount)
            .map { index -> preferences.getChildAt(index) }
            .mapNotNull { view -> view as? PreferenceView }
            .map { preference -> preference.clicks().map { preference } }
            .let { preferences -> Observable.merge(preferences) }

    override fun aboutLongClicks(): Observable<*> = about.longClicks()

    override fun nightModeSelected(): Observable<Int> = nightModeDialog.adapter.menuItemClicks

    override fun nightStartSelected(): Observable<Pair<Int, Int>> = startTimeSelectedSubject

    override fun nightEndSelected(): Observable<Pair<Int, Int>> = endTimeSelectedSubject

    override fun textSizeSelected(): Observable<Int> = textSizeDialog.adapter.menuItemClicks

    override fun sendDelaySelected(): Observable<Int> = sendDelayDialog.adapter.menuItemClicks

    override fun signatureSet(): Observable<String> = signatureSubject

    override fun mmsSizeSelected(): Observable<Int> = mmsSizeDialog.adapter.menuItemClicks

    override fun render(state: SettingsState) {
        themePreview.setBackgroundTint(state.theme)
        night.summary = state.nightModeSummary
        nightModeDialog.adapter.selectedItem = state.nightModeId
        nightStart.setVisible(state.nightModeId == Preferences.NIGHT_MODE_AUTO)
        nightStart.summary = state.nightStart
        nightEnd.setVisible(state.nightModeId == Preferences.NIGHT_MODE_AUTO)
        nightEnd.summary = state.nightEnd

        black.setVisible(state.nightModeId != Preferences.NIGHT_MODE_OFF)
        black.checkbox.isChecked = state.black

        autoEmoji.checkbox.isChecked = state.autoEmojiEnabled

        delayed.summary = state.sendDelaySummary
        sendDelayDialog.adapter.selectedItem = state.sendDelayId

        delivery.checkbox.isChecked = state.deliveryEnabled

        signature.summary = state.signature

        textSize.summary = state.textSizeSummary
        textSizeDialog.adapter.selectedItem = state.textSizeId
        systemFont.checkbox.isChecked = state.systemFontEnabled

        unicode.checkbox.isChecked = state.stripUnicodeEnabled
        mobileOnly.checkbox.isChecked = state.mobileOnly

        mmsSize.summary = state.maxMmsSizeSummary
        mmsSizeDialog.adapter.selectedItem = state.maxMmsSizeId

        when (state.syncProgress) {
            is SyncRepository.SyncProgress.Idle -> syncingProgress.isVisible = false
            is SyncRepository.SyncProgress.Running -> {
                syncingProgress.isVisible = true
                syncingProgress.max = state.syncProgress.max
                progressAnimator.apply { setIntValues(syncingProgress.progress, state.syncProgress.progress) }.start()
                syncingProgress.isIndeterminate = state.syncProgress.indeterminate
            }
        }
    }

    // TODO change this to a PopupWindow
    override fun showNightModeDialog() = nightModeDialog.show(activity!!)

    override fun showStartTimePicker(hour: Int, minute: Int) {
        TimePickerDialog(activity, TimePickerDialog.OnTimeSetListener { _, newHour, newMinute ->
            startTimeSelectedSubject.onNext(Pair(newHour, newMinute))
        }, hour, minute, DateFormat.is24HourFormat(activity)).show()
    }

    override fun showEndTimePicker(hour: Int, minute: Int) {
        TimePickerDialog(activity, TimePickerDialog.OnTimeSetListener { _, newHour, newMinute ->
            endTimeSelectedSubject.onNext(Pair(newHour, newMinute))
        }, hour, minute, DateFormat.is24HourFormat(activity)).show()
    }

    override fun showTextSizePicker() = textSizeDialog.show(activity!!)

    override fun showDelayDurationDialog() = sendDelayDialog.show(activity!!)

    override fun showSignatureDialog(signature: String) = signatureDialog.setText(signature).show()

    override fun showMmsSizePicker() = mmsSizeDialog.show(activity!!)

    override fun showSwipeActions() {
        router.pushController(RouterTransaction.with(SwipeActionsController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

    override fun showThemePicker() {
        router.pushController(RouterTransaction.with(ThemePickerController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

    override fun showAbout() {
        router.pushController(RouterTransaction.with(AboutController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

    override fun showSyncInfo(prefs: Preferences) {
        AlertDialog.Builder(activity!!)
                .setTitle("Information")
                .setMessage(R.string.bluetooth_alert_sync_info)
                .setPositiveButton(R.string.bluetooth_alert_button_ok, null)
                .setNeutralButton(R.string.bluetooth_alert_button_dismiss) { _, _ -> prefs.bluetooth_sync_dismiss.set(true) }
                .show()
    }

}