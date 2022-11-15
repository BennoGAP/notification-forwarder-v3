package org.groebl.sms.feature.settings.speechbubble

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

class SpeechBubblePresenter @Inject constructor(
    //context: Context,
    private val context: Context,
    private val prefs: Preferences,
    private val widgetManager: WidgetManager
) : QkPresenter<SpeechBubbleView, SpeechBubbleState>(SpeechBubbleState()) {

    init {
        disposables += prefs.bubbleColorInvert.asObservable()
            .subscribe { bubbleColorInvert -> newState { copy(bubbleColorInvert = bubbleColorInvert) }
                widgetManager.updateTheme()}

        val bubbleStyleLabels = context.resources.getStringArray(R.array.settings_bubble_style)
        val bubbleStyleIds = context.resources.getIntArray(R.array.settings_bubble_style_ids)
        disposables += prefs.bubbleStyle.asObservable()
            .subscribe { bubbleStyle ->
                val index = bubbleStyleIds.indexOf(bubbleStyle)
                newState { copy(bubbleStyleSummary = bubbleStyleLabels[index], bubbleStyleIds = bubbleStyle) }
            }

    }

    override fun bindIntents(view: SpeechBubbleView) {
        super.bindIntents(view)

        view.preferenceClicks()
            .autoDisposable(view.scope())
            .subscribe { preference ->
                when (preference.id) {
                    R.id.bubbleColorInvert -> prefs.bubbleColorInvert.set(!prefs.bubbleColorInvert.get())
                    R.id.bubbleStyle -> view.showBubbleStylePicker()
                }
            }

        view.speechBubbleSelected()
            .autoDisposable(view.scope())
            .subscribe(prefs.bubbleStyle::set)

        view.styleOriginalSelected()
            .autoDisposable(view.scope())
            .subscribe { prefs.bubbleStyle.set(Preferences.BUBBLE_STYLE_ORIGINAL) }

        view.styleIosSelected()
            .autoDisposable(view.scope())
            .subscribe{ prefs.bubbleStyle.set(Preferences.BUBBLE_STYLE_IOS) }

        view.styleSimpleSelected()
            .autoDisposable(view.scope())
            .subscribe{ prefs.bubbleStyle.set(Preferences.BUBBLE_STYLE_SIMPLE) }

        view.styleTriangleSelected()
            .autoDisposable(view.scope())
            .subscribe{ prefs.bubbleStyle.set(Preferences.BUBBLE_STYLE_TRIANGLE) }

    }

}