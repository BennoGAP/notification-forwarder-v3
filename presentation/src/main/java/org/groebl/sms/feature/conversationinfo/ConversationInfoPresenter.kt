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
package org.groebl.sms.feature.conversationinfo

import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.groebl.sms.common.Navigator
import org.groebl.sms.common.base.QkPresenter
import org.groebl.sms.extensions.asObservable
import org.groebl.sms.interactor.DeleteConversations
import org.groebl.sms.interactor.MarkArchived
import org.groebl.sms.interactor.MarkUnarchived
import org.groebl.sms.manager.PermissionManager
import org.groebl.sms.model.Conversation
import org.groebl.sms.repository.ConversationRepository
import org.groebl.sms.repository.MessageRepository
import javax.inject.Inject
import javax.inject.Named

class ConversationInfoPresenter @Inject constructor(
        @Named("threadId") threadId: Long,
        messageRepo: MessageRepository,
        private val conversationRepo: ConversationRepository,
        private val deleteConversations: DeleteConversations,
        private val markArchived: MarkArchived,
        private val markUnarchived: MarkUnarchived,
        private val navigator: Navigator,
        private val permissionManager: PermissionManager
) : QkPresenter<ConversationInfoView, ConversationInfoState>(
        ConversationInfoState(threadId = threadId, media = messageRepo.getPartsForConversation(threadId))
) {

    private val conversation: Subject<Conversation> = BehaviorSubject.create()

    init {
        disposables += conversationRepo.getConversationAsync(threadId)
                .asObservable()
                .filter { conversation -> conversation.isLoaded }
                .doOnNext { conversation ->
                    if (!conversation.isValid) {
                        newState { copy(hasError = true) }
                    }
                }
                .filter { conversation -> conversation.isValid }
                .filter { conversation -> conversation.id != 0L }
                .subscribe(conversation::onNext)

        disposables += markArchived
        disposables += markUnarchived
        disposables += deleteConversations

        // Update the recipients whenever they change
        disposables += conversation
                .map { conversation -> conversation.recipients }
                .distinctUntilChanged()
                .subscribe { recipients -> newState { copy(recipients = recipients) } }

        // Update conversation title whenever it changes
        disposables += conversation
                .map { conversation -> conversation.name }
                .distinctUntilChanged()
                .subscribe { name -> newState { copy(name = name) } }

        // Update the view's archived state whenever it changes
        disposables += conversation
                .map { conversation -> conversation.archived }
                .distinctUntilChanged()
                .subscribe { archived -> newState { copy(archived = archived) } }

        // Update the view's blocked state whenever it changes
        disposables += conversation
                .map { conversation -> conversation.blocked }
                .distinctUntilChanged()
                .subscribe { blocked -> newState { copy(blocked = blocked) } }
    }

    override fun bindIntents(view: ConversationInfoView) {
        super.bindIntents(view)

        // Show the conversation title dialog
        view.nameClicks()
                .withLatestFrom(conversation) { _, conversation -> conversation }
                .map { conversation -> conversation.name }
                .autoDisposable(view.scope())
                .subscribe(view::showNameDialog)

        // Set the conversation title
        view.nameChanges()
                .withLatestFrom(conversation) { name, conversation ->
                    conversationRepo.setConversationName(conversation.id, name)
                }
                .autoDisposable(view.scope())
                .subscribe()

        // Show the notifications settings for the conversation
        view.notificationClicks()
                .withLatestFrom(conversation) { _, conversation -> conversation }
                .autoDisposable(view.scope())
                .subscribe { conversation -> navigator.showNotificationSettings(conversation.id) }

        // Show the theme settings for the conversation
        view.themeClicks()
                .withLatestFrom(conversation) { _, conversation -> conversation }
                .autoDisposable(view.scope())
                .subscribe { conversation -> view.showThemePicker(conversation.id) }

        // Toggle the archived state of the conversation
        view.archiveClicks()
                .withLatestFrom(conversation) { _, conversation -> conversation }
                .autoDisposable(view.scope())
                .subscribe { conversation ->
                    when (conversation.archived) {
                        true -> markUnarchived.execute(listOf(conversation.id))
                        false -> markArchived.execute(listOf(conversation.id))
                    }
                }

        // Toggle the blocked state of the conversation
        view.blockClicks()
                .withLatestFrom(conversation) { _, conversation -> conversation }
                .autoDisposable(view.scope())
                .subscribe { conversation -> view.showBlockingDialog(listOf(conversation.id), !conversation.blocked) }

        // Show the delete confirmation dialog
        view.deleteClicks()
                .filter { permissionManager.isDefaultSms().also { if (!it) navigator.showDefaultSmsDialog() } }
                .autoDisposable(view.scope())
                .subscribe { view.showDeleteDialog() }

        // Delete the conversation
        view.confirmDelete()
                .withLatestFrom(conversation) { _, conversation -> conversation }
                .autoDisposable(view.scope())
                .subscribe { conversation -> deleteConversations.execute(listOf(conversation.id)) }
    }

}