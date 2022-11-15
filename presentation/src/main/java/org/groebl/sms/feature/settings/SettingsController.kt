/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
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
package org.groebl.sms.feature.settings

import android.animation.ObjectAnimator
import android.app.TimePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.text.format.DateFormat
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.RouterTransaction
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.longClicks

import org.groebl.sms.R
import org.groebl.sms.common.MenuItem
import org.groebl.sms.common.QkChangeHandler
import org.groebl.sms.common.QkDialog
import org.groebl.sms.common.base.QkController
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.extensions.animateLayoutChanges
import org.groebl.sms.common.util.extensions.resolveThemeColor
import org.groebl.sms.common.util.extensions.setBackgroundTint
import org.groebl.sms.common.util.extensions.setVisible
import org.groebl.sms.common.widget.PreferenceView
import org.groebl.sms.common.widget.TextInputDialog
import org.groebl.sms.feature.settings.about.AboutController
import org.groebl.sms.feature.settings.autodelete.AutoDeleteDialog
import org.groebl.sms.feature.settings.simconfigure.SimConfigureController
import org.groebl.sms.feature.settings.speechbubble.SpeechBubbleController
import org.groebl.sms.feature.settings.swipe.SwipeActionsController
import org.groebl.sms.feature.themepicker.ThemePickerController
import org.groebl.sms.injection.appComponent
import org.groebl.sms.repository.SyncRepository
import org.groebl.sms.util.Preferences
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.android.synthetic.main.settings_controller.*
import kotlinx.android.synthetic.main.settings_switch_widget.view.*
import kotlinx.android.synthetic.main.settings_theme_widget.*
import kotlinx.android.synthetic.main.settings_chevron_widget.view.*
import javax.inject.Inject
import kotlin.coroutines.resume

class SettingsController : QkController<SettingsView, SettingsState, SettingsPresenter>(), SettingsView {

    @Inject lateinit var context: Context
    @Inject lateinit var colors: Colors
    @Inject lateinit var nightModeDialog: QkDialog
    @Inject lateinit var textSizeDialog: QkDialog
    @Inject lateinit var sendDelayDialog: QkDialog
    @Inject lateinit var mmsSizeDialog: QkDialog

    @Inject override lateinit var presenter: SettingsPresenter

    private val signatureDialog: TextInputDialog by lazy {
        TextInputDialog(activity!!, context.getString(R.string.settings_signature_title), signatureSubject::onNext)
    }
    private val autoDeleteDialog: AutoDeleteDialog by lazy {
        AutoDeleteDialog(activity!!, autoDeleteSubject::onNext)
    }

    private val startTimeSelectedSubject: Subject<Pair<Int, Int>> = PublishSubject.create()
    private val endTimeSelectedSubject: Subject<Pair<Int, Int>> = PublishSubject.create()
    private val signatureSubject: Subject<String> = PublishSubject.create()
    private val autoDeleteSubject: Subject<Int> = PublishSubject.create()

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

        when (Build.VERSION.SDK_INT >= 29) {
            true -> nightModeDialog.adapter.setData(R.array.night_modes)
            false -> nightModeDialog.adapter.data = context.resources.getStringArray(R.array.night_modes)
                    .mapIndexed { index, title -> MenuItem(title, index) }
                    .drop(1)
        }
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

        val states = arrayOf(
            intArrayOf(android.R.attr.state_activated),
            intArrayOf(-android.R.attr.state_activated))

        val textTertiary = view.context.resolveThemeColor(android.R.attr.textColorTertiary)
        val imageTintList = ColorStateList(states, intArrayOf(colors.theme().theme, textTertiary))

        speechBubble.chevron.imageTintList = imageTintList
        simConfigure.chevron.imageTintList = imageTintList
        notifications.chevron.imageTintList = imageTintList
        swipeActions.chevron.imageTintList = imageTintList
        about.chevron.imageTintList = imageTintList
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

    override fun signatureChanged(): Observable<String> = signatureSubject

    override fun autoDeleteChanged(): Observable<Int> = autoDeleteSubject

    override fun mmsSizeSelected(): Observable<Int> = mmsSizeDialog.adapter.menuItemClicks

    override fun render(state: SettingsState) {
        themePreview.setBackgroundTint(state.theme)

        speechBubble.chevron.setImageResource(R.drawable.ic_chevron_right_black_24dp)
        simConfigure.chevron.setImageResource(R.drawable.ic_chevron_right_black_24dp)
        notifications.chevron.setImageResource(R.drawable.ic_chevron_right_black_24dp)
        swipeActions.chevron.setImageResource(R.drawable.ic_chevron_right_black_24dp)
        about.chevron.setImageResource(R.drawable.ic_chevron_right_black_24dp)

        night.value = state.nightModeSummary
        nightModeDialog.adapter.selectedItem = state.nightModeId
        nightStart.setVisible(state.nightModeId == Preferences.NIGHT_MODE_AUTO)
        nightStart.value = state.nightStart
        nightEnd.setVisible(state.nightModeId == Preferences.NIGHT_MODE_AUTO)
        nightEnd.value = state.nightEnd

        black.setVisible(state.nightModeId != Preferences.NIGHT_MODE_OFF)
        black.checkbox.isChecked = state.black
        gray.setVisible(state.nightModeId != Preferences.NIGHT_MODE_ON)
        gray.checkbox.isChecked = state.gray

        autoEmoji.checkbox.isChecked = state.autoEmojiEnabled

        delayed.value = state.sendDelaySummary
        sendDelayDialog.adapter.selectedItem = state.sendDelayId

        delivery.checkbox.isChecked = state.deliveryEnabled

       /* signature.summary = state.signature.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.settings_signature_summary)*/
        signature.value = state.signature.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.settings_signature_empty_value)

        textSize.value = state.textSizeSummary
        textSizeDialog.adapter.selectedItem = state.textSizeId

        autoColor.checkbox.isChecked = state.autoColor

        grayAvatar.checkbox.isChecked = state.grayAvatar

        separator.checkbox.isChecked = state.separator

        systemFont.checkbox.isChecked = state.systemFontEnabled

        unicode.checkbox.isChecked = state.stripUnicodeEnabled
        mobileOnly.checkbox.isChecked = state.mobileOnly

        autoDelete.value = when (state.autoDelete) {
            0 -> context.getString(R.string.settings_auto_delete_never)
            else -> context.resources.getQuantityString(
                    R.plurals.settings_auto_delete_summary, state.autoDelete, state.autoDelete)
        }

        longAsMms.checkbox.isChecked = state.longAsMms

        optOut.checkbox.isChecked = state.optOut

        mmsSize.value = state.maxMmsSizeSummary
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
        TimePickerDialog(activity, { _, newHour, newMinute ->
            startTimeSelectedSubject.onNext(Pair(newHour, newMinute))
        }, hour, minute, DateFormat.is24HourFormat(activity)).show()
    }

    override fun showEndTimePicker(hour: Int, minute: Int) {
        TimePickerDialog(activity, { _, newHour, newMinute ->
            endTimeSelectedSubject.onNext(Pair(newHour, newMinute))
        }, hour, minute, DateFormat.is24HourFormat(activity)).show()
    }

    override fun showTextSizePicker() = textSizeDialog.show(activity!!)

    override fun showDelayDurationDialog() = sendDelayDialog.show(activity!!)

    override fun showSignatureDialog(signature: String) = signatureDialog.setText(signature).show()

    override fun showAutoDeleteDialog(days: Int) = autoDeleteDialog.setExpiry(days).show()

    override suspend fun showAutoDeleteWarningDialog(messages: Int): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Boolean> { cont ->
            AlertDialog.Builder(activity!!)
                    .setTitle(R.string.settings_auto_delete_warning)
                    .setMessage(context.resources.getString(R.string.settings_auto_delete_warning_message, messages))
                    .setOnCancelListener { cont.resume(false) }
                    .setNegativeButton(R.string.button_cancel) { _, _ -> cont.resume(false) }
                    .setPositiveButton(R.string.button_yes) { _, _ -> cont.resume(true) }
                    .show()
        }
    }

    override fun showMmsSizePicker() = mmsSizeDialog.show(activity!!)

    override fun showSpeechBubble() {
        router.pushController(RouterTransaction.with(SpeechBubbleController())
            .pushChangeHandler(QkChangeHandler())
            .popChangeHandler(QkChangeHandler()))
    }

    override fun showSimConfigure() {
        router.pushController(RouterTransaction.with(SimConfigureController())
            .pushChangeHandler(QkChangeHandler())
            .popChangeHandler(QkChangeHandler()))
    }

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