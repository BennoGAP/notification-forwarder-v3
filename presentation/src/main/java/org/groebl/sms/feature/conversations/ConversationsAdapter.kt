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
package org.groebl.sms.feature.conversations

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import org.groebl.sms.R
import org.groebl.sms.common.Navigator
import org.groebl.sms.common.base.QkRealmAdapter
import org.groebl.sms.common.base.QkViewHolder
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.DateFormatter
import org.groebl.sms.common.util.extensions.resolveThemeColor
import org.groebl.sms.common.util.extensions.setTint
import org.groebl.sms.model.Conversation
import org.groebl.sms.util.PhoneNumberUtils
import kotlinx.android.synthetic.main.conversation_list_item.*
import kotlinx.android.synthetic.main.conversation_list_item.view.*
import javax.inject.Inject

class ConversationsAdapter @Inject constructor(
    private val colors: Colors,
    private val context: Context,
    private val dateFormatter: DateFormatter,
    private val navigator: Navigator,
    private val phoneNumberUtils: PhoneNumberUtils
) : QkRealmAdapter<Conversation>() {

    init {
        // This is how we access the threadId for the swipe actions
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.conversation_list_item, parent, false)

        if (viewType == 1) {
            val textColorPrimary = parent.context.resolveThemeColor(android.R.attr.textColorPrimary)

            view.title.setTypeface(view.title.typeface, Typeface.BOLD)

            view.snippet.setTypeface(view.snippet.typeface, Typeface.BOLD)
            view.snippet.setTextColor(textColorPrimary)
            view.snippet.maxLines = 5

            view.unread.isVisible = true

            view.date.setTypeface(view.date.typeface, Typeface.BOLD)
            view.date.setTextColor(textColorPrimary)
        }

        return QkViewHolder(view).apply {
            view.setOnClickListener {
                val conversation = getItem(adapterPosition) ?: return@setOnClickListener
                when (toggleSelection(conversation.id, false)) {
                    true -> view.isActivated = isSelected(conversation.id)
                    false -> navigator.showConversation(conversation.id)
                }
            }
            view.setOnLongClickListener {
                val conversation = getItem(adapterPosition) ?: return@setOnLongClickListener true
                toggleSelection(conversation.id)
                view.isActivated = isSelected(conversation.id)
                true
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val conversation = getItem(position) ?: return

        // If the last message wasn't incoming, then the colour doesn't really matter anyway
        val lastMessage = conversation.lastMessage
        val recipient = when {
            conversation.recipients.size == 1 || lastMessage == null -> conversation.recipients.firstOrNull()
            else -> conversation.recipients.find { recipient ->
                phoneNumberUtils.compare(recipient.address, lastMessage.address)
            }
        }
        val theme = colors.theme(recipient).theme

        holder.containerView.isActivated = isSelected(conversation.id)

        holder.avatars.recipients = conversation.recipients
        holder.title.collapseEnabled = conversation.recipients.size > 1
        holder.title.text = buildSpannedString {
            append(conversation.getTitle())
            if (conversation.draft.isNotEmpty()) {
                color(theme) { append(" " + context.getString(R.string.main_draft)) }
            }
        }

        val subscription = subs.find { sub -> sub.subscriptionId == lastMessage?.subId }
        holder.simIndex.text = subscription?.simSlotIndex?.plus(1)?.toString()
        holder.sim.setVisible(subscription != null && subs.size > 1)
        holder.simIndex.setVisible(subscription != null && subs.size > 1)
        val simColor = when (subscription?.simSlotIndex?.plus(1)?.toString()) {
            "1" -> colors.colorForSim(context, 1)
            "2" -> colors.colorForSim(context, 2)
            "3" -> colors.colorForSim(context, 3)
            else -> colors.colorForSim(context, 1)
        }
        if (prefs.simColor.get()) {
            holder.sim.setTint(simColor)
        }

        holder.date.text = conversation.date.takeIf { it > 0 }?.let(dateFormatter::getConversationTimestamp)

        holder.snippet.text = when {
            conversation.draft.isNotEmpty() -> conversation.draft.replace(
                Regex("(\r\n|\r|\n)"),
                " "
            )
                .replace("  ", " ")
            conversation.me -> context.getString(
                R.string.main_sender_you,
                conversation.snippet?.replace(Regex("(\r\n|\r|\n)"), " ")?.replace("  ", " ")
            )
            else -> conversation.snippet?.replace(Regex("(\r\n|\r|\n)"), " ")?.replace("  ", " ")
        }
        holder.pinned.isVisible = conversation.pinned
        holder.unread.setTint(theme)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id ?: -1
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position)?.unread == false) 0 else 1
    }
}
