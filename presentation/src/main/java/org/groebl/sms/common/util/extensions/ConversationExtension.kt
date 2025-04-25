package org.groebl.sms.common.util.extensions

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import org.groebl.sms.R
import org.groebl.sms.common.widget.AvatarView
import org.groebl.sms.model.Conversation
import timber.log.Timber

fun Conversation.getThemedIcon(context: Context, width: Int, height: Int): IconCompat {

    var icon : IconCompat? = null
    Timber.v("Photo not found, creating default icon")
    val inflater = LayoutInflater.from(context)
    val layout = FrameLayout(context)
    layout.layout(0, 0, width, height)
    layout.layoutParams = FrameLayout.LayoutParams(width, height)
    val view = inflater.inflate(R.layout.group_avatar_view, layout)
    view.layoutParams = FrameLayout.LayoutParams(width, height)
    val avatar1 = view.findViewById<AvatarView>(R.id.avatar1)
    val avatar2 = view.findViewById<AvatarView>(R.id.avatar2)
    val avatar1Frame = view.findViewById<FrameLayout>(R.id.avatar1Frame)

    avatar1Frame.setBackgroundTint(when (recipients.size > 1) {
        true -> context.resolveThemeColor(android.R.attr.windowBackground)
        false -> context.getColorCompat(android.R.color.transparent)
    })
    avatar1Frame.updateLayoutParams<LayoutParams> {
        matchConstraintPercentWidth = if (recipients.size > 1) 0.75f else 1.0f
    }
    avatar2.isVisible = recipients.size > 1


    recipients.getOrNull(0).run(avatar1::setRecipient)
    recipients.getOrNull(1).run(avatar2::setRecipient)

    view.apply {
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
        layout(0, 0, measuredWidth, measuredHeight)
    }

    Timber.v("w: ${view.measuredWidth}; h: ${view.measuredHeight}")
    val bitmap = createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    view.draw(canvas)

    icon = IconCompat.createWithBitmap(bitmap)
    return icon
}