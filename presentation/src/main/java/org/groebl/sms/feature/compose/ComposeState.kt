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

import org.groebl.sms.compat.SubscriptionInfoCompat
import org.groebl.sms.model.Attachment
import org.groebl.sms.model.Conversation
import org.groebl.sms.model.Message
import org.groebl.sms.model.Recipient
import io.realm.RealmResults

data class ComposeState(
    val hasError: Boolean = false,
    val editingMode: Boolean = false,
    val threadId: Long = 0,
    val selectedChips: List<Recipient> = ArrayList(),
    val sendAsGroup: Boolean = true,
    val conversationtitle: String = "",
    val loading: Boolean = false,
    val query: String = "",
    val searchSelectionId: Long = -1,
    val searchSelectionPosition: Int = 0,
    val searchResults: Int = 0,
    val messages: Pair<Conversation, RealmResults<Message>>? = null,
    val selectedMessages: Int = 0,
    val selectedMessagesHaveText: Boolean = false,
    val scheduled: Long = 0,
    val attachments: List<Attachment> = listOf(),
    val attaching: Boolean = false,
    val scheduling: Boolean = false,
    val remaining: String = "",
    val subscription: SubscriptionInfoCompat? = null,
    val canSend: Boolean = false,
    val validRecipientNumbers: Int = 1,
    val audioMsgRecording: Boolean = false,
    val saveDraft: Boolean = true,
)