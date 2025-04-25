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

import org.groebl.sms.blocking.BlockingClient
import org.groebl.sms.extensions.mapNotNull
import org.groebl.sms.manager.NotificationManager
import org.groebl.sms.manager.ShortcutManager
import org.groebl.sms.repository.ConversationRepository
import org.groebl.sms.repository.MessageRepository
import org.groebl.sms.util.Preferences
import io.reactivex.Flowable
import timber.log.Timber
import javax.inject.Inject

class ReceiveSms @Inject constructor(
    private val conversationRepo: ConversationRepository,
    private val blockingClient: BlockingClient,
    private val prefs: Preferences,
    private val messageRepo: MessageRepository,
    private val notificationManager: NotificationManager,
    private val updateBadge: UpdateBadge,
    private val shortcutManager: ShortcutManager
) : Interactor<Long>() {

    override fun buildObservable(params: Long): Flowable<*> {
        return Flowable.just(params)
            .mapNotNull { messageRepo.getMessage(it) }
            .mapNotNull {
                var action = blockingClient.shouldBlock(it.address).blockingGet()

                if (action !is BlockingClient.Action.Block) {
                    Timber.v("number is not blocked, check if content is blocked")
                    // Check if we should block it because of its content
                    action = blockingClient.getActionFromContent(it.body).blockingGet()
                }

                when {
                    ((action is BlockingClient.Action.Block) && prefs.drop.get()) ->  {
                        // blocked and 'drop blocked.' remove from db and don't continue
                        Timber.v("address/message is blocked and drop blocked is on. dropped")
                        messageRepo.deleteMessages(listOf(it.id))
                        return@mapNotNull null
                    }
                    action is BlockingClient.Action.Block -> {
                        Timber.v("blocked for reason: ${action.reason}")
                        when (action.reason) {
                            "message" -> {
                                Timber.v("message is blocked and deleted")
                                messageRepo.deleteMessages(listOf(it.id))
                                return@mapNotNull null
                            }
                            else -> {
                                Timber.v("address is blocked")
                                messageRepo.markRead(listOf(it.threadId))
                                conversationRepo.markBlocked(
                                    listOf(it.threadId),
                                    prefs.blockingManager.get(),
                                    action.reason
                                )
                            }
                        }
                    }
                    action is BlockingClient.Action.Unblock -> {
                        // unblock
                        Timber.v("unblock conversation if blocked")
                        conversationRepo.markUnblocked(it.threadId)
                    }
                }

                // update and fetch conversation
                conversationRepo.updateConversations(it.threadId)
                conversationRepo.getOrCreateConversation(it.threadId)
            }
            .mapNotNull {
                // don't notify (continue) for blocked conversations
                if (it.blocked) {
                    Timber.v("no notifications for blocked")
                    return@mapNotNull null
                }

                // unarchive conversation if necessary
                if (it.archived) {
                    Timber.v("conversation unarchived")
                    conversationRepo.markUnarchived(it.id)
                }

                // update/create notification
                Timber.v("update/create notification")
                notificationManager.update(it.id)

                // update shortcuts
                Timber.v("update shortcuts")
                shortcutManager.updateShortcuts()
                shortcutManager.reportShortcutUsed(it.id)

                // update the badge and widget
                Timber.v("update badge and widget")
                updateBadge.buildObservable(Unit)
            }
    }

}
