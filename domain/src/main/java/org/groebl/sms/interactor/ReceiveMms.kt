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
package org.groebl.sms.interactor

import android.net.Uri
import org.groebl.sms.extensions.mapNotNull
import org.groebl.sms.manager.ExternalBlockingManager
import org.groebl.sms.manager.NotificationManager
import org.groebl.sms.repository.ConversationRepository
import org.groebl.sms.repository.MessageRepository
import org.groebl.sms.repository.SyncRepository
import io.reactivex.Flowable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReceiveMms @Inject constructor(
        private val conversationRepo: ConversationRepository,
        private val externalBlockingManager: ExternalBlockingManager,
        private val syncManager: SyncRepository,
        private val messageRepo: MessageRepository,
        private val notificationManager: NotificationManager,
        private val updateBadge: UpdateBadge
) : Interactor<Uri>() {

    override fun buildObservable(params: Uri): Flowable<*> {
        return Flowable.just(params)
                .mapNotNull { uri -> syncManager.syncMessage(uri) } // Sync the message
                .filter { message ->
                    // Because we use the smsmms library for receiving and storing MMS, we'll need
                    // to check if it should be blocked after we've pulled it into realm. If it
                    // turns out that it should be blocked, then delete it
                    // TODO Don't store blocked messages in the first place
                    !externalBlockingManager.shouldBlock(message.address).blockingGet().also { blocked ->
                        if (blocked) messageRepo.deleteMessages(message.id)
                    }
                }
                .doOnNext { message -> conversationRepo.updateConversations(message.threadId) } // Update the conversation
                .mapNotNull { message -> conversationRepo.getOrCreateConversation(message.threadId) } // Map message to conversation
                .filter { conversation -> !conversation.blocked } // Don't notify for blocked conversations
                .doOnNext { conversation -> if (conversation.archived) conversationRepo.markUnarchived(conversation.id) } // Unarchive conversation if necessary
                .map { conversation -> conversation.id } // Map to the id because [delay] will put us on the wrong thread
                .delay(1, TimeUnit.SECONDS) // Wait one second before trying to notify, in case the foreground app marks it as read first
                .doOnNext { threadId -> notificationManager.update(threadId) } // Update the notification
                .flatMap { updateBadge.buildObservable(Unit) } // Update the badge
    }

}