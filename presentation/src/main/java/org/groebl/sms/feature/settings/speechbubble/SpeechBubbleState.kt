package org.groebl.sms.feature.settings.speechbubble

data class SpeechBubbleState(
    val bubbleColorInvert: Boolean = true,
    val bubbleStyleIds: Int = 0,
    val bubbleStyleSummary: String = "Original"
)