package org.groebl.sms.feature.settings.speechbubble

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import org.groebl.sms.R
import org.groebl.sms.common.base.QkController
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.extensions.*
import org.groebl.sms.common.widget.PreferenceView
import org.groebl.sms.feature.compose.BubbleUtils
import org.groebl.sms.injection.appComponent
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import kotlinx.android.synthetic.main.settings_controller.preferences
import kotlinx.android.synthetic.main.settings_switch_widget.view.*
import kotlinx.android.synthetic.main.speech_bubble_controller.*
import org.groebl.sms.common.QkDialog
import org.groebl.sms.util.Preferences
import javax.inject.Inject

class SpeechBubbleController : QkController<SpeechBubbleView, SpeechBubbleState, SpeechBubblePresenter>(), SpeechBubbleView {

    @Inject override lateinit var presenter: SpeechBubblePresenter
    @Inject lateinit var actionsDialog: QkDialog
    @Inject lateinit var colors: Colors
    @Inject lateinit var context: Context

    init {
        appComponent.inject(this)
        layoutRes = R.layout.speech_bubble_controller

    }

    override fun onViewCreated() {
        actionsDialog.adapter.setData(R.array.settings_bubble_style, R.array.settings_bubble_style_ids)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.settings_speech_bubble_title)
        showBackButton(true)
    }

    override fun preferenceClicks(): Observable<PreferenceView> = (0 until preferences.childCount)
        .map { index -> preferences.getChildAt(index) }
        .mapNotNull { view -> view as? PreferenceView }
        .map { preference -> preference.clicks().map { preference } }
        .let { preferences -> Observable.merge(preferences) }

    override fun speechBubbleSelected(): Observable<Int> = actionsDialog.adapter.menuItemClicks

    override fun showBubbleStylePicker() = actionsDialog.show(activity!!)

    override fun styleOriginalSelected(): Observable<*> = styleOriginal.clicks()

    override fun styleIosSelected(): Observable<*> = styleIos.clicks()

    override fun styleSimpleSelected(): Observable<*> = styleSimple.clicks()

    override fun styleTriangleSelected(): Observable<*> = styleTriangle.clicks()

    override fun render(state: SpeechBubbleState) {
        bubbleColorInvert.checkbox.isChecked = state.bubbleColorInvert

        bubbleStyle.value = state.bubbleStyleSummary
        actionsDialog.adapter.selectedItem = state.bubbleStyleIds

        styleOriginalСheck.isActivated = (state.bubbleStyleIds == Preferences.BUBBLE_STYLE_ORIGINAL)
        styleIosСheck.isActivated = (state.bubbleStyleIds == Preferences.BUBBLE_STYLE_IOS)
        styleSimpleСheck.isActivated = (state.bubbleStyleIds == Preferences.BUBBLE_STYLE_SIMPLE)
        styleTriangleСheck.isActivated = (state.bubbleStyleIds == Preferences.BUBBLE_STYLE_TRIANGLE)
        val states = arrayOf(
            intArrayOf(android.R.attr.state_activated),
            intArrayOf(-android.R.attr.state_activated))
        val text = context.resolveThemeColor(android.R.attr.textColorTertiary)

        arrayOf(styleOriginalСheck, styleIosСheck, styleSimpleСheck, styleTriangleСheck).forEach {
            it.imageTintList = ColorStateList(states, intArrayOf(colors.theme().theme, text))
        }

        styleOriginalBubbleOne.setBackgroundResource(
            BubbleUtils.getBubble(
                emojiOnly = false,
                canGroupWithPrevious = false,
                canGroupWithNext = true,
                isMe = false,
                style = Preferences.BUBBLE_STYLE_ORIGINAL
            )
        )
        styleOriginalBubbleTwo.setBackgroundResource(
            BubbleUtils.getBubble(
                emojiOnly = false,
                canGroupWithPrevious = true,
                canGroupWithNext = false,
                isMe = false,
                style = Preferences.BUBBLE_STYLE_ORIGINAL
            )
        )

        styleOriginalBubbleThree.setBackgroundResource(
            BubbleUtils.getBubble(
                emojiOnly = false,
                canGroupWithPrevious = false,
                canGroupWithNext = false,
                isMe = true,
                style = Preferences.BUBBLE_STYLE_ORIGINAL
            )
        )

        styleIosBubbleOne.setBackgroundResource(
            BubbleUtils.getBubble(
                emojiOnly = false,
                canGroupWithPrevious = false,
                canGroupWithNext = true,
                isMe = false,
                style = Preferences.BUBBLE_STYLE_IOS
            )
        )
        styleIosBubbleTwo.setBackgroundResource(
            BubbleUtils.getBubble(
                emojiOnly = false,
                canGroupWithPrevious = true,
                canGroupWithNext = false,
                isMe = false,
                style = Preferences.BUBBLE_STYLE_IOS
            )
        )

        styleIosBubbleThree.setBackgroundResource(
            BubbleUtils.getBubble(
                emojiOnly = false,
                canGroupWithPrevious = false,
                canGroupWithNext = false,
                isMe = true,
                style = Preferences.BUBBLE_STYLE_IOS
            )
        )
        val paddingTop = context.resources.getDimensionPixelOffset(R.dimen.bubble_padding_top)
        val paddingBottom = context.resources.getDimensionPixelOffset(R.dimen.bubble_padding_bottom)
        val paddingLeft = context.resources.getDimensionPixelOffset(R.dimen.bubble_padding_left)
        val paddingRight = context.resources.getDimensionPixelOffset(R.dimen.bubble_padding_right)
        styleIosBubbleOne.setPadding(paddingRight, paddingTop, paddingLeft, paddingBottom)
        styleIosBubbleTwo.setPadding(paddingRight, paddingTop, paddingLeft, paddingBottom)
        styleIosBubbleThree.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)

        styleSimpleBubbleOne.setBackgroundResource(
            BubbleUtils.getBubble(
                emojiOnly = false,
                canGroupWithPrevious = false,
                canGroupWithNext = true,
                isMe = false,
                style = Preferences.BUBBLE_STYLE_SIMPLE
            )
        )
        styleSimpleBubbleTwo.setBackgroundResource(
            BubbleUtils.getBubble(
                emojiOnly = false,
                canGroupWithPrevious = true,
                canGroupWithNext = false,
                isMe = false,
                style = Preferences.BUBBLE_STYLE_SIMPLE
            )
        )

        styleSimpleBubbleThree.setBackgroundResource(
            BubbleUtils.getBubble(
                emojiOnly = false,
                canGroupWithPrevious = false,
                canGroupWithNext = false,
                isMe = true,
                style = Preferences.BUBBLE_STYLE_SIMPLE
            )
        )

        styleTriangleBubbleOne.setBackgroundResource(
            BubbleUtils.getBubble(
                emojiOnly = false,
                canGroupWithPrevious = false,
                canGroupWithNext = true,
                isMe = false,
                style = Preferences.BUBBLE_STYLE_TRIANGLE
            )
        )
        styleTriangleBubbleTwo.setBackgroundResource(
            BubbleUtils.getBubble(
                emojiOnly = false,
                canGroupWithPrevious = true,
                canGroupWithNext = false,
                isMe = false,
                style = Preferences.BUBBLE_STYLE_TRIANGLE
            )
        )

        styleTriangleBubbleThree.setBackgroundResource(
            BubbleUtils.getBubble(
                emojiOnly = false,
                canGroupWithPrevious = false,
                canGroupWithNext = false,
                isMe = true,
                style = Preferences.BUBBLE_STYLE_TRIANGLE
            )
        )

        themedActivity?.colors?.theme()?.let { theme ->
            if (state.bubbleColorInvert) {
                styleOriginalBubbleOne.setTextColor(styleOriginalBubbleOne.context.resolveThemeColor(android.R.attr.textColorPrimary))
                styleOriginalBubbleOne.setBackgroundTint(styleOriginalBubbleOne.context.resolveThemeColor(R.attr.bubbleColor))
                styleOriginalBubbleTwo.setTextColor(styleOriginalBubbleTwo.context.resolveThemeColor(android.R.attr.textColorPrimary))
                styleOriginalBubbleTwo.setBackgroundTint(styleOriginalBubbleTwo.context.resolveThemeColor(R.attr.bubbleColor))
                styleOriginalBubbleThree.setTextColor(theme.textPrimary)
                styleOriginalBubbleThree.setBackgroundTint(theme.theme)

                styleIosBubbleOne.setTextColor(styleIosBubbleOne.context.resolveThemeColor(android.R.attr.textColorPrimary))
                styleIosBubbleOne.setBackgroundTint(styleIosBubbleOne.context.resolveThemeColor(R.attr.bubbleColor))
                styleIosBubbleTwo.setTextColor(styleIosBubbleTwo.context.resolveThemeColor(android.R.attr.textColorPrimary))
                styleIosBubbleTwo.setBackgroundTint(styleIosBubbleTwo.context.resolveThemeColor(R.attr.bubbleColor))
                styleIosBubbleThree.setTextColor(theme.textPrimary)
                styleIosBubbleThree.setBackgroundTint(theme.theme)

                styleSimpleBubbleOne.setTextColor(styleSimpleBubbleOne.context.resolveThemeColor(android.R.attr.textColorPrimary))
                styleSimpleBubbleOne.setBackgroundTint(styleSimpleBubbleOne.context.resolveThemeColor(R.attr.bubbleColor))
                styleSimpleBubbleTwo.setTextColor(styleSimpleBubbleTwo.context.resolveThemeColor(android.R.attr.textColorPrimary))
                styleSimpleBubbleTwo.setBackgroundTint(styleSimpleBubbleTwo.context.resolveThemeColor(R.attr.bubbleColor))
                styleSimpleBubbleThree.setTextColor(theme.textPrimary)
                styleSimpleBubbleThree.setBackgroundTint(theme.theme)

                styleTriangleBubbleOne.setTextColor(styleTriangleBubbleOne.context.resolveThemeColor(android.R.attr.textColorPrimary))
                styleTriangleBubbleOne.setBackgroundTint(styleTriangleBubbleOne.context.resolveThemeColor(R.attr.bubbleColor))
                styleTriangleBubbleTwo.setTextColor(styleTriangleBubbleTwo.context.resolveThemeColor(android.R.attr.textColorPrimary))
                styleTriangleBubbleTwo.setBackgroundTint(styleTriangleBubbleTwo.context.resolveThemeColor(R.attr.bubbleColor))
                styleTriangleBubbleThree.setTextColor(theme.textPrimary)
                styleTriangleBubbleThree.setBackgroundTint(theme.theme)
            } else {
                styleOriginalBubbleOne.setTextColor(theme.textPrimary)
                styleOriginalBubbleOne.setBackgroundTint(theme.theme)
                styleOriginalBubbleTwo.setTextColor(theme.textPrimary)
                styleOriginalBubbleTwo.setBackgroundTint(theme.theme)
                styleOriginalBubbleThree.setTextColor(styleOriginalBubbleThree.context.resolveThemeColor(android.R.attr.textColorPrimary))
                styleOriginalBubbleThree.setBackgroundTint(styleOriginalBubbleThree.context.resolveThemeColor(R.attr.bubbleColor))

                styleIosBubbleOne.setTextColor(theme.textPrimary)
                styleIosBubbleOne.setBackgroundTint(theme.theme)
                styleIosBubbleTwo.setTextColor(theme.textPrimary)
                styleIosBubbleTwo.setBackgroundTint(theme.theme)
                styleIosBubbleThree.setTextColor(styleIosBubbleThree.context.resolveThemeColor(android.R.attr.textColorPrimary))
                styleIosBubbleThree.setBackgroundTint(styleIosBubbleThree.context.resolveThemeColor(R.attr.bubbleColor))

                styleSimpleBubbleOne.setTextColor(theme.textPrimary)
                styleSimpleBubbleOne.setBackgroundTint(theme.theme)
                styleSimpleBubbleTwo.setTextColor(theme.textPrimary)
                styleSimpleBubbleTwo.setBackgroundTint(theme.theme)
                styleSimpleBubbleThree.setTextColor(styleSimpleBubbleThree.context.resolveThemeColor(android.R.attr.textColorPrimary))
                styleSimpleBubbleThree.setBackgroundTint(styleSimpleBubbleThree.context.resolveThemeColor(R.attr.bubbleColor))

                styleTriangleBubbleOne.setTextColor(theme.textPrimary)
                styleTriangleBubbleOne.setBackgroundTint(theme.theme)
                styleTriangleBubbleTwo.setTextColor(theme.textPrimary)
                styleTriangleBubbleTwo.setBackgroundTint(theme.theme)
                styleTriangleBubbleThree.setTextColor(styleTriangleBubbleThree.context.resolveThemeColor(android.R.attr.textColorPrimary))
                styleTriangleBubbleThree.setBackgroundTint(styleTriangleBubbleThree.context.resolveThemeColor(R.attr.bubbleColor))
            }

        }

    }

}