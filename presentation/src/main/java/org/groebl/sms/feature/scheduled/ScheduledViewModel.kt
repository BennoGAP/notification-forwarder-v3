package org.groebl.sms.feature.scheduled

import android.content.Context
import org.groebl.sms.R
import org.groebl.sms.common.Navigator
import org.groebl.sms.common.base.QkViewModel
import org.groebl.sms.interactor.SendScheduledMessage
import org.groebl.sms.repository.ScheduledMessageRepository
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import org.groebl.sms.common.util.ClipboardUtils
import org.groebl.sms.interactor.DeleteScheduledMessages
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class ScheduledViewModel @Inject constructor(
    private val context: Context,
    private val navigator: Navigator,
    private val scheduledMessageRepo: ScheduledMessageRepository,
    private val sendScheduledMessageInteractor: SendScheduledMessage,
    private val deleteScheduledMessagesInteractor: DeleteScheduledMessages,
) : QkViewModel<ScheduledView, ScheduledState>(ScheduledState(
    scheduledMessages = scheduledMessageRepo.getScheduledMessages()
)) {


    override fun bindView(view: ScheduledView) {
        super.bindView(view)

        // update the state when the message selected count changes
        view.messagesSelectedIntent
            .map { selection -> selection.size }
            .autoDisposable(view.scope())
            .subscribe { newState { copy(selectedMessages = it) } }

        // toggle select all / select none
        view.optionsItemIntent
            .filter { it == R.id.select_all }
            .autoDisposable(view.scope())
            .subscribe { view.toggleSelectAll() }

        // show the delete message dialog if one or more messages selected
        view.optionsItemIntent
            .filter { it == R.id.delete }
            .withLatestFrom(view.messagesSelectedIntent) { _, selectedMessages -> selectedMessages }
            .autoDisposable(view.scope())
            .subscribe { view.showDeleteDialog(it) }

        // copy the selected message text to the clipboard
        view.optionsItemIntent
            .filter { it == R.id.copy }
            .withLatestFrom(view.messagesSelectedIntent) { _, selectedMessages -> selectedMessages }
            .autoDisposable(view.scope())
            .subscribe {
                val messages = it
                    .mapNotNull(scheduledMessageRepo::getScheduledMessage)
                    .sortedBy { it.date }   // same order as messages on screen
                val text = when (messages.size) {
                    1 -> messages.first().body
                    else -> messages.fold(StringBuilder()) { acc, message ->
                        if (acc.isNotEmpty() && message.body.isNotEmpty())
                            acc.append("\n\n")
                        acc.append(message.body)
                    }
                }

                ClipboardUtils.copy(context, text.toString())
            }

        // send the messages now menu item selected
        view.optionsItemIntent
            .filter { it == R.id.send_now }
            .withLatestFrom(view.messagesSelectedIntent) { _, selectedMessages -> selectedMessages }
            .autoDisposable(view.scope())
            .subscribe { view.showSendNowDialog(it) }

        // edit message menu item selected
        view.optionsItemIntent
            .filter { it == R.id.edit_message }
            .withLatestFrom(view.messagesSelectedIntent) { _, selectedMessage -> selectedMessage.first() }
            .autoDisposable(view.scope())
            .subscribe { view.showEditMessageDialog(it) }

        // delete message(s) (fired after the confirmation dialog has been shown)
        view.deleteScheduledMessages
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .autoDisposable(view.scope())
            .subscribe {
                deleteScheduledMessagesInteractor.execute(it)
                view.clearSelection()
            }

        // send message(s) now (fired after the confirmation dialog has been shown)
        view.sendScheduledMessages
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .autoDisposable(view.scope())
            .subscribe {
                it.forEach { sendScheduledMessageInteractor.execute(it) }
                view.clearSelection()
            }


        // edit message (fired after the confirmation dialog has been shown)
        view.editScheduledMessage
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .autoDisposable(view.scope())
            .subscribe {
                scheduledMessageRepo.getScheduledMessage(it)?.let {
                    navigator.showCompose(it)
                    scheduledMessageRepo.deleteScheduledMessage(it.id)
                }
                view.clearSelection()
            }

        // navigate back or unselect
        view.optionsItemIntent
            .filter { it == android.R.id.home }
            .map { Unit }
            .mergeWith(view.backPressedIntent)
            .withLatestFrom(state) { _, state -> state }
            .autoDisposable(view.scope())
            .subscribe {
                when {
                    (it.selectedMessages > 0) -> view.clearSelection()
                    else -> view.finishActivity()
                }
            }

        view.composeIntent
            .autoDisposable(view.scope())
            .subscribe {
                navigator.showCompose(mode = "scheduling")
                view.clearSelection()
            }

    }
}
