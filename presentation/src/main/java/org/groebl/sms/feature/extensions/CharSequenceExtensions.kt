package org.groebl.sms.feature.extensions

import android.text.Spannable
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiCompat.REPLACE_STRATEGY_ALL
import androidx.emoji2.text.EmojiSpan


fun CharSequence.isEmojiOnly(considerWhitespace: Boolean = false): Boolean {
    val cs =
        if (considerWhitespace) this
        else this.replace(Regex("[\\s\n\r]"), "")

    if (cs.isEmpty())
        return false

    val emojiCompat = EmojiCompat.get()

    if (emojiCompat.loadState != EmojiCompat.LOAD_STATE_SUCCEEDED) return false

    val spannable = emojiCompat.process(
        cs,
        0,
        (cs.length - 1),
        Int.MAX_VALUE,
        REPLACE_STRATEGY_ALL
    )

    if (spannable !is Spannable) return false

    val emojiLengthSum = spannable
        .getSpans(0, spannable.length, EmojiSpan::class.java)
        .fold(0) { acc, emojiSpan ->
            acc + (spannable.getSpanEnd(emojiSpan) - spannable.getSpanStart(emojiSpan))
        }

    return emojiLengthSum == cs.length
}