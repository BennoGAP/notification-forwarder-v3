package org.groebl.sms.feature.settings

import android.content.Context
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.rxkotlin.plusAssign
import org.groebl.sms.R
import org.groebl.sms.common.Navigator
import org.groebl.sms.common.base.QkPresenter
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.DateFormatter
import org.groebl.sms.common.util.extensions.makeToast
import org.groebl.sms.interactor.SyncMessages
import org.groebl.sms.repository.SyncRepository
import org.groebl.sms.util.NightModeManager
import org.groebl.sms.util.Preferences
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SettingsPresenter @Inject constructor(
        colors: Colors,
        syncRepo: SyncRepository,
        private val context: Context,
        private val dateFormatter: DateFormatter,
        private val navigator: Navigator,
        private val nightModeManager: NightModeManager,
        private val prefs: Preferences,
        private val syncMessages: SyncMessages
        ) : QkPresenter<SettingsView, SettingsState>(SettingsState()) {

    init {
        disposables += colors.themeObservable()
                .subscribe { theme -> newState { copy(theme = theme.theme) } }

        val nightModeLabels = context.resources.getStringArray(R.array.night_modes)
        disposables += prefs.nightMode.asObservable()
                .subscribe { nightMode ->
                    newState { copy(nightModeSummary = nightModeLabels[nightMode], nightModeId = nightMode) }
                }

        disposables += prefs.nightStart.asObservable()
                .map { time -> nightModeManager.parseTime(time) }
                .map { calendar -> calendar.timeInMillis }
                .map { millis -> dateFormatter.getTimestamp(millis) }
                .subscribe { nightStart -> newState { copy(nightStart = nightStart) } }

        disposables += prefs.nightEnd.asObservable()
                .map { time -> nightModeManager.parseTime(time) }
                .map { calendar -> calendar.timeInMillis }
                .map { millis -> dateFormatter.getTimestamp(millis) }
                .subscribe { nightEnd -> newState { copy(nightEnd = nightEnd) } }

        disposables += prefs.black.asObservable()
                .subscribe { black -> newState { copy(black = black) } }

        disposables += prefs.notifications().asObservable()
                .subscribe { enabled -> newState { copy(notificationsEnabled = enabled) } }

        disposables += prefs.autoEmoji.asObservable()
                .subscribe { enabled -> newState { copy(autoEmojiEnabled = enabled) } }

        val delayedSendingLabels = context.resources.getStringArray(R.array.delayed_sending_labels)
        disposables += prefs.sendDelay.asObservable()
                .subscribe { id -> newState { copy(sendDelaySummary = delayedSendingLabels[id], sendDelayId = id) } }

        disposables += prefs.delivery.asObservable()
                .subscribe { enabled -> newState { copy(deliveryEnabled = enabled) } }

        disposables += prefs.signature.asObservable()
                .subscribe { signature -> newState { copy(signature = signature) } }

        val textSizeLabels = context.resources.getStringArray(R.array.text_sizes)
        disposables += prefs.textSize.asObservable()
                .subscribe { textSize ->
                    newState { copy(textSizeSummary = textSizeLabels[textSize], textSizeId = textSize) }
                }

        disposables += prefs.systemFont.asObservable()
                .subscribe { enabled -> newState { copy(systemFontEnabled = enabled) } }

        disposables += prefs.unicode.asObservable()
                .subscribe { enabled -> newState { copy(stripUnicodeEnabled = enabled) } }

        disposables += prefs.mobileOnly.asObservable()
                .subscribe { enabled -> newState { copy(mobileOnly = enabled) } }

        val mmsSizeLabels = context.resources.getStringArray(R.array.mms_sizes)
        val mmsSizeIds = context.resources.getIntArray(R.array.mms_sizes_ids)
        disposables += prefs.mmsSize.asObservable()
                .subscribe { maxMmsSize ->
                    val index = mmsSizeIds.indexOf(maxMmsSize)
                    newState { copy(maxMmsSizeSummary = mmsSizeLabels[index], maxMmsSizeId = maxMmsSize) }
                }

        disposables += syncRepo.syncProgress
                .sample(16, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .subscribe { syncProgress -> newState { copy(syncProgress = syncProgress) } }

        disposables += syncMessages
    }

    override fun bindIntents(view: SettingsView) {
        super.bindIntents(view)

        view.preferenceClicks()
                .autoDisposable(view.scope())
                .subscribe {
                    Timber.v("Preference click: ${context.resources.getResourceName(it.id)}")

                    when (it.id) {
                        R.id.theme -> view.showThemePicker()

                        R.id.night -> view.showNightModeDialog()

                        R.id.nightStart -> {
                            val date = nightModeManager.parseTime(prefs.nightStart.get())
                            view.showStartTimePicker(date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE))
                        }

                        R.id.nightEnd -> {
                            val date = nightModeManager.parseTime(prefs.nightEnd.get())
                            view.showEndTimePicker(date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE))
                        }

                        R.id.black -> prefs.black.set(!prefs.black.get())

                        R.id.autoEmoji -> prefs.autoEmoji.set(!prefs.autoEmoji.get())

                        R.id.notifications -> navigator.showNotificationSettings()

                        R.id.swipeActions -> view.showSwipeActions()

                        R.id.delayed -> view.showDelayDurationDialog()

                        R.id.delivery -> prefs.delivery.set(!prefs.delivery.get())

                        R.id.signature -> view.showSignatureDialog(prefs.signature.get())

                        R.id.textSize -> view.showTextSizePicker()

                        R.id.systemFont -> prefs.systemFont.set(!prefs.systemFont.get())

                        R.id.unicode -> prefs.unicode.set(!prefs.unicode.get())

                        R.id.mobileOnly -> prefs.mobileOnly.set(!prefs.mobileOnly.get())

                        R.id.mmsSize -> view.showMmsSizePicker()

                        R.id.sync -> {
                            syncMessages.execute(Unit)
                            if(prefs.bluetooth_enabled.get() && !prefs.bluetooth_sync_dismiss.get()) { view.showSyncInfo(prefs) }
                        }

                        R.id.about -> view.showAbout()
                    }
                }

        view.aboutLongClicks()
                .map { !prefs.logging.get() }
                .doOnNext { enabled -> prefs.logging.set(enabled) }
                .autoDisposable(view.scope())
                .subscribe { enabled ->
                    context.makeToast(when (enabled) {
                        true -> R.string.settings_logging_enabled
                        false -> R.string.settings_logging_disabled
                    })
                }

        view.nightModeSelected()
                .autoDisposable(view.scope())
                .subscribe{ nightModeManager.updateNightMode(it) }

        view.nightStartSelected()
                .autoDisposable(view.scope())
                .subscribe { nightModeManager.setNightStart(it.first, it.second) }

        view.nightEndSelected()
                .autoDisposable(view.scope())
                .subscribe { nightModeManager.setNightEnd(it.first, it.second) }

        view.textSizeSelected()
                .autoDisposable(view.scope())
                .subscribe(prefs.textSize::set)

        view.sendDelaySelected()
                .autoDisposable(view.scope())
                .subscribe{ prefs.sendDelay.set(it) }

        view.signatureSet()
                .doOnNext(prefs.signature::set)
                .autoDisposable(view.scope())
                .subscribe()

        view.mmsSizeSelected()
                .autoDisposable(view.scope())
                .subscribe(prefs.mmsSize::set)
    }

}