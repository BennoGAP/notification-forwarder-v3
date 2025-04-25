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

import android.content.res.Resources
import org.groebl.sms.manager.SpeakManager
import org.groebl.sms.domain.R
import org.groebl.sms.extensions.mapNotNull
import org.groebl.sms.repository.ConversationRepository
import org.groebl.sms.repository.MessageRepository
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject

class SpeakThreads @Inject constructor(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: MessageRepository,
) : Interactor<List<Long>>() {

    companion object {
        private var noMessagesStr = "No messages"       // default value

        fun setNoMessagesString(newNoMessagesString: String) {
            noMessagesStr = newNoMessagesString
        }
    }

    override fun buildObservable(threadIds: List<Long>): Flowable<*> {
        val speakManager = SpeakManager()

        if (threadIds.isEmpty())
              return Single.just(0)
                  .doOnSubscribe { speakManager.startSpeakSession(noMessagesStr) }
                  .map { speakManager.speak(noMessagesStr) }
                  .doOnTerminate { speakManager.endSpeakSession() }
                  .toFlowable()

        return Flowable.fromIterable(threadIds)
            .doOnSubscribe { speakManager.startSpeakSession("threads:" + threadIds.sorted().joinToString()) }
            .mapNotNull { threadId -> conversationRepo.getConversationAndLastSenderContactName(threadId) }
            .map { conversationAndSender ->
                    if (speakManager.speakConversationLastSms(conversationAndSender))
                        messageRepo.markSeen(conversationAndSender.first!!.id)
            }
            .doOnTerminate { speakManager.endSpeakSession() }
    }

}