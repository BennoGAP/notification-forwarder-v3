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
package org.groebl.sms.feature.scheduled

import android.content.Context
import org.groebl.sms.R
import org.groebl.sms.common.Navigator
import org.groebl.sms.common.base.QkViewModel
import org.groebl.sms.common.util.ClipboardUtils
import org.groebl.sms.common.util.extensions.makeToast
import org.groebl.sms.interactor.SendScheduledMessage
import org.groebl.sms.repository.MessageRepository
import org.groebl.sms.repository.ScheduledMessageRepository
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.rxkotlin.withLatestFrom
import javax.inject.Inject

class ScheduledViewModel @Inject constructor(
        private val context: Context,
        private val messageRepo: MessageRepository,
        private val navigator: Navigator,
        private val scheduledMessageRepo: ScheduledMessageRepository,
        private val sendScheduledMessage: SendScheduledMessage
) : QkViewModel<ScheduledView, ScheduledState>(ScheduledState(
        scheduledMessages = scheduledMessageRepo.getScheduledMessages()
)) {

    override fun bindView(view: ScheduledView) {
        super.bindView(view)

        view.messageClickIntent
                .autoDisposable(view.scope())
                .subscribe { view.showMessageOptions() }

        view.messageMenuIntent
                .withLatestFrom(view.messageClickIntent) { itemId, messageId ->
                    when (itemId) {
                        0 -> sendScheduledMessage.execute(messageId)
                        1 -> scheduledMessageRepo.getScheduledMessage(messageId)?.let { message ->
                            ClipboardUtils.copy(context, message.body)
                            context.makeToast(R.string.toast_copied)
                        }
                        2 -> scheduledMessageRepo.deleteScheduledMessage(messageId)
                    }
                    Unit
                }
                .autoDisposable(view.scope())
                .subscribe()

        view.composeIntent
                .autoDisposable(view.scope())
                .subscribe { navigator.showCompose() }

    }

}