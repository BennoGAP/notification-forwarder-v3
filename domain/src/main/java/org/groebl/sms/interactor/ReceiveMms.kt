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
import org.groebl.sms.blocking.BlockingClient
import org.groebl.sms.extensions.mapNotNull
import org.groebl.sms.manager.ActiveConversationManager
import org.groebl.sms.manager.NotificationManager
import org.groebl.sms.repository.ConversationRepository
import org.groebl.sms.repository.MessageRepository
import org.groebl.sms.repository.SyncRepository
import org.groebl.sms.util.Preferences
import io.reactivex.Flowable
import timber.log.Timber
import javax.inject.Inject

class ReceiveMms @Inject constructor(
    private val activeConversationManager: ActiveConversationManager,
    private val conversationRepo: ConversationRepository,
    private val blockingClient: BlockingClient,
    private val prefs: Preferences,
    private val syncManager: SyncRepository,
    private val messageRepo: MessageRepository,
    private val notificationManager: NotificationManager,
    private val updateBadge: UpdateBadge
) : Interactor<Uri>() {

    override fun buildObservable(params: Uri): Flowable<*> {
        return Flowable.just(params)
                .mapNotNull(syncManager::syncMessage) // Sync the message
                .doOnNext { message ->
                    // TODO: Ideally this is done when we're saving the MMS to ContentResolver
                    // This change can be made once we move the MMS storing code to the Data module
                    if (activeConversationManager.getActiveConversation() == message.threadId) {
                        messageRepo.markRead(listOf(message.threadId))
                    }
                }
                .mapNotNull { message ->
                    // Because we use the smsmms library for receiving and storing MMS, we'll need
                    // to check if it should be blocked after we've pulled it into realm. If it
                    // turns out that it should be dropped, then delete it
                    // TODO Don't store blocked messages in the first place
                    var action = blockingClient.shouldBlock(message.address).blockingGet()

                    if (action !is BlockingClient.Action.Block) {
                        // Check if we should block it because of its content
                        action = blockingClient.getActionFromContent(message.body).blockingGet()
                    }

                    when {
                        ((action is BlockingClient.Action.Block) && prefs.drop.get()) ->  {
                            // blocked and 'drop blocked.' remove from db and don't continue
                            Timber.v("address/message is blocked and drop blocked is on. dropped")
                            messageRepo.deleteMessages(listOf(message.id))
                            return@mapNotNull null
                        }
                        action is BlockingClient.Action.Block -> {
                            when (action.reason) {
                                "message" -> {
                                    Timber.v("message is blocked and deleted")
                                    messageRepo.deleteMessages(listOf(message.id))
                                    return@mapNotNull null
                                }
                                else -> {
                                    Timber.v("address is blocked")
                                    messageRepo.markRead(listOf(message.threadId))
                                    conversationRepo.markBlocked(listOf(message.threadId), prefs.blockingManager.get(), action.reason)
                                }
                            }
                        }
                        action is BlockingClient.Action.Unblock -> {
                            // unblock
                            Timber.v("unblock conversation if blocked")
                            conversationRepo.markUnblocked(message.threadId)
                        }
                        else -> Unit
                    }

                    message
                }
                .doOnNext { message ->
                    conversationRepo.updateConversations(message.threadId) // Update the conversation
                }
                .mapNotNull { message ->
                    conversationRepo.getOrCreateConversation(message.threadId) // Map message to conversation
                }
                .filter { conversation -> !conversation.blocked } // Don't notify for blocked conversations
                .doOnNext { conversation ->
                    // Unarchive conversation if necessary
                    if (conversation.archived) conversationRepo.markUnarchived(conversation.id)
                }
                .map { conversation -> conversation.id } // Map to the id because [delay] will put us on the wrong thread
                .doOnNext(notificationManager::update) // Update the notification
                .flatMap { updateBadge.buildObservable(Unit) } // Update the badge
    }

}