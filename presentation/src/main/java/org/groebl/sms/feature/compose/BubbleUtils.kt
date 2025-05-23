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
package org.groebl.sms.feature.compose

import org.groebl.sms.R
import org.groebl.sms.model.Message
import org.groebl.sms.util.Preferences
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object BubbleUtils {

    const val TIMESTAMP_THRESHOLD = 10

    fun canGroup(message: Message, other: Message?): Boolean {
        if (other == null) return false
        val diff = TimeUnit.MILLISECONDS.toMinutes(abs(message.date - other.date))
        return message.compareSender(other) && diff < TIMESTAMP_THRESHOLD
    }

    fun getBubble(emojiOnly: Boolean, canGroupWithPrevious: Boolean, canGroupWithNext: Boolean, isMe: Boolean, style: Int = 0): Int {
        if (style == Preferences.BUBBLE_STYLE_IOS) {
            return when {
                emojiOnly -> R.drawable.message_emoji
                !canGroupWithPrevious && canGroupWithNext -> R.drawable.message_ios_no_last
                canGroupWithPrevious && canGroupWithNext -> R.drawable.message_ios_no_last
                canGroupWithPrevious && !canGroupWithNext -> if (isMe) R.drawable.message_ios_out_last else R.drawable.message_ios_in_last
                else -> if (isMe) R.drawable.message_ios_out_last else R.drawable.message_ios_in_last
            }
        } else if (style == Preferences.BUBBLE_STYLE_SIMPLE) {
            return when {
                emojiOnly -> R.drawable.message_emoji
                !canGroupWithPrevious && canGroupWithNext -> R.drawable.message_only
                canGroupWithPrevious && canGroupWithNext -> R.drawable.message_only
                canGroupWithPrevious && !canGroupWithNext -> if (isMe) R.drawable.message_simple_out_last else R.drawable.message_simple_in_last
                else -> if (isMe) R.drawable.message_simple_out_last else R.drawable.message_simple_in_last
            }
        } else if (style == Preferences.BUBBLE_STYLE_TRIANGLE) {
            return when {
                emojiOnly -> R.drawable.message_emoji
                !canGroupWithPrevious && canGroupWithNext -> if (isMe) R.drawable.message_triangle_out_only else R.drawable.message_triangle_in_only
                canGroupWithPrevious && canGroupWithNext -> if (isMe) R.drawable.message_triangle_out_only else R.drawable.message_triangle_in_only
                canGroupWithPrevious && !canGroupWithNext -> if (isMe) R.drawable.message_triangle_out_last else R.drawable.message_triangle_in_last
                else -> if (isMe) R.drawable.message_triangle_out_last else R.drawable.message_triangle_in_last
            }
        } else {
        return when {
            emojiOnly -> R.drawable.message_emoji
            !canGroupWithPrevious && canGroupWithNext -> if (isMe) R.drawable.message_out_first else R.drawable.message_in_first
            canGroupWithPrevious && canGroupWithNext -> if (isMe) R.drawable.message_out_middle else R.drawable.message_in_middle
            canGroupWithPrevious && !canGroupWithNext -> if (isMe) R.drawable.message_out_last else R.drawable.message_in_last
            else -> R.drawable.message_only
        }
    }

}

}