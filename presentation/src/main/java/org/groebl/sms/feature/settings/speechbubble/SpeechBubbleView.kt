package org.groebl.sms.feature.settings.speechbubble

import org.groebl.sms.common.base.QkViewContract
import org.groebl.sms.common.widget.PreferenceView
import io.reactivex.Observable

interface SpeechBubbleView : QkViewContract<SpeechBubbleState> {

    fun preferenceClicks(): Observable<PreferenceView>
    fun speechBubbleSelected(): Observable<Int>

    fun showBubbleStylePicker()
    fun styleOriginalSelected(): Observable<*>
    fun styleIosSelected(): Observable<*>
    fun styleSimpleSelected(): Observable<*>
    fun styleTriangleSelected(): Observable<*>

}