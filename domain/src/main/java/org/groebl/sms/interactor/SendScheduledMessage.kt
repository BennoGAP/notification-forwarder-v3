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

import android.content.Context
import android.net.Uri
import org.groebl.sms.compat.TelephonyCompat
import org.groebl.sms.extensions.mapNotNull
import org.groebl.sms.model.Attachment
import org.groebl.sms.repository.ScheduledMessageRepository
import io.reactivex.Flowable
import io.reactivex.rxkotlin.toFlowable
import io.realm.RealmList
import javax.inject.Inject

class SendScheduledMessage @Inject constructor(
    private val context: Context,
    private val scheduledMessageRepo: ScheduledMessageRepository,
    private val deleteScheduledMessagesInteractor: DeleteScheduledMessages,
    private val sendMessage: SendMessage
) : Interactor<Long>() {

    override fun buildObservable(params: Long): Flowable<*> {
        return Flowable.just(params)
            .mapNotNull(scheduledMessageRepo::getScheduledMessage)
            .flatMap { message ->
                if (message.sendAsGroup) {
                    listOf(message)
                } else {
                    message.recipients.map { recipient -> message.copy(recipients = RealmList(recipient)) }
                }.toFlowable()
            }
            .map { message ->
                val threadId = TelephonyCompat.getOrCreateThreadId(context, message.recipients)
                val attachments = message.attachments.mapNotNull(Uri::parse).map { Attachment(context, it) }
                SendMessage.Params(message.subId, threadId, message.recipients, message.body, attachments)
            }
            .flatMap(sendMessage::buildObservable)
            .doOnNext { deleteScheduledMessagesInteractor.execute(listOf(params)) }
    }

}