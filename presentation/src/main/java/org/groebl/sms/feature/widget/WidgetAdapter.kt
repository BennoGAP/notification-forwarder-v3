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
package org.groebl.sms.feature.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.italic
import com.bumptech.glide.Glide
import org.groebl.sms.R
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.DateFormatter
import org.groebl.sms.common.util.extensions.dpToPx
import org.groebl.sms.common.util.extensions.getColorCompat
import org.groebl.sms.common.util.extensions.setBackgroundTint
import org.groebl.sms.feature.compose.ComposeActivity
import org.groebl.sms.feature.main.MainActivity
import org.groebl.sms.injection.appComponent
import org.groebl.sms.model.Contact
import org.groebl.sms.model.Conversation
import org.groebl.sms.model.PhoneNumber
import org.groebl.sms.receiver.StartActivityFromWidgetReceiver
import org.groebl.sms.repository.ConversationRepository
import org.groebl.sms.util.Preferences
import org.groebl.sms.util.tryOrNull
import javax.inject.Inject

class WidgetAdapter(intent: Intent) : RemoteViewsService.RemoteViewsFactory {

    companion object {
        private const val MAX_CONVERSATIONS_COUNT = 25
    }

    @Inject lateinit var context: Context
    @Inject lateinit var colors: Colors
    @Inject lateinit var conversationRepo: ConversationRepository
    @Inject lateinit var dateFormatter: DateFormatter
    @Inject lateinit var prefs: Preferences

    private val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID)
    private val smallWidget = intent.getBooleanExtra("small_widget", false)
    private var conversations: List<Conversation> = listOf()
    private val appWidgetManager by lazy { AppWidgetManager.getInstance(context) }

    private val night get() = prefs.night.get()
    private val black get() = prefs.black.get()
    private val gray get() = prefs.gray.get()
    private val theme get() = colors.theme()
    private val background
        get() = context.getColorCompat(when {
            night && black -> R.color.black
            night && !black -> R.color.backgroundDark
            !night && gray -> R.color.backgroundGray
            else -> R.color.white
        })
    private val textPrimary
        get() = context.getColorCompat(if (night) R.color.textPrimaryDark else R.color.textPrimary)
    private val textSecondary
        get() = context.getColorCompat(if (night) R.color.textSecondaryDark else R.color.textSecondary)
    private val textTertiary
        get() = context.getColorCompat(if (night) R.color.textTertiaryDark else R.color.textTertiary)

    override fun onCreate() {
        appComponent.inject(this)
    }

    override fun onDataSetChanged() {
        conversations = conversationRepo.getConversationsSnapshot(prefs.unreadAtTop.get())

        val remoteViews = RemoteViews(context.packageName, R.layout.widget)
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews)
    }

    /**
     * Returns the number of rows to show. If there are more conversations than the max count,
     * we'll return the max count + 1, where the last row just shows "View more conversations"
     */
    override fun getCount(): Int {
        val count = conversations.size.coerceAtMost(MAX_CONVERSATIONS_COUNT)
        val shouldShowViewMore = count < conversations.size
        return count + if (shouldShowViewMore) 1 else 0
    }

    override fun getViewAt(position: Int): RemoteViews {
        return when {
            position >= MAX_CONVERSATIONS_COUNT -> getOverflowView()
            else -> getConversationView(position)
        }
    }

    private fun getConversationView(position: Int): RemoteViews {
        val conversation = conversations[position]

        val remoteViews = RemoteViews(context.packageName, R.layout.widget_list_item)

        // Avatar
        remoteViews.setViewVisibility(R.id.avatar, if (smallWidget) View.GONE else View.VISIBLE)
        if (!prefs.grayAvatar.get()) {
            remoteViews.setInt(R.id.avatar, "setBackgroundColor", theme.theme)
        } else {
            remoteViews.setInt(R.id.avatar, "setBackgroundResource", R.drawable.circle)
        }
        remoteViews.setTextColor(R.id.initial, theme.textPrimary)
        remoteViews.setInt(R.id.icon, "setColorFilter", theme.textPrimary)
        remoteViews.setInt(R.id.avatarMask, "setColorFilter", background)

        val contact = conversation.recipients.map { recipient ->
            recipient.contact ?: Contact().apply { numbers.add(PhoneNumber().apply { address = recipient.address }) }
        }.firstOrNull()

        // Use the icon if there's no name, otherwise show an initial
        if (contact?.name.orEmpty().isNotEmpty()) {
            remoteViews.setTextViewText(R.id.initial, contact?.name?.substring(0, 1))
            remoteViews.setViewVisibility(R.id.icon, View.GONE)
        } else {
            remoteViews.setTextViewText(R.id.initial, null)
            remoteViews.setViewVisibility(R.id.icon, View.VISIBLE)
        }

        remoteViews.setImageViewBitmap(R.id.photo, null)
        val futureGet = Glide.with(context)
                .asBitmap()
                .load(contact?.photoUri)
                .submit(48.dpToPx(context), 48.dpToPx(context))
        tryOrNull(false) { remoteViews.setImageViewBitmap(R.id.photo, futureGet.get()) }

        // Name
        remoteViews.setTextColor(R.id.name, textPrimary)
        remoteViews.setTextViewText(R.id.name, boldText(buildSpannedString {
            append(conversation.getTitle())
        }, conversation.unread))

        // Date
        val timestamp = conversation.date.takeIf { it > 0 }?.let(dateFormatter::getConversationTimestamp)
        remoteViews.setTextColor(R.id.date, if (conversation.unread) textPrimary else textTertiary)
        remoteViews.setTextViewText(R.id.date, boldText(timestamp, conversation.unread))

        // Snippet
        val snippet = when {
            conversation.draft.isNotEmpty() -> context.getString(
                R.string.main_sender_draft,
                conversation.draft
            )
            conversation.me -> context.getString(R.string.main_sender_you, conversation.snippet)
            else -> conversation.snippet
        }
        remoteViews.setTextColor(R.id.snippet, if (conversation.unread) textPrimary else textTertiary)
        remoteViews.setTextViewText(R.id.snippet, boldText(snippet, conversation.unread))
        remoteViews.setTextViewText(R.id.snippet, italicText(snippet, conversation.draft.isNotEmpty()))

        // set fill-in intent to be used for current item
        remoteViews.setOnClickFillInIntent(
            R.id.conversation,
            Intent()
                .putExtra("activityToStart", StartActivityFromWidgetReceiver.COMPOSE_ACTIVITY)
                .putExtra("threadId", conversation.id)
        )

        return remoteViews
    }

    private fun getOverflowView(): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.widget_loading)
        view.setTextColor(R.id.loadingText, textSecondary)
        view.setTextViewText(R.id.loadingText, context.getString(R.string.widget_more))
        view.setOnClickFillInIntent(
            R.id.loadingText,
            Intent()
                .putExtra("activityToStart", StartActivityFromWidgetReceiver.MAIN_ACTIVITY)
        )
        return view
    }

    private fun boldText(text: CharSequence?, shouldBold: Boolean): CharSequence? = when {
        shouldBold -> SpannableStringBuilder()
                .bold { append(text) }
        else -> text
    }

    private fun italicText(text: CharSequence?, shouldBold: Boolean): CharSequence? = when {
        shouldBold -> SpannableStringBuilder()
            .italic { append(text) }
        else -> text
    }

    override fun getLoadingView(): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.widget_loading)
        view.setTextViewText(R.id.loadingText, context.getText(R.string.widget_loading))
        return view
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun onDestroy() {
    }

}