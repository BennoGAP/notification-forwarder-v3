package org.groebl.sms.feature.blocking.regexps

import org.groebl.sms.common.Navigator
import org.groebl.sms.common.base.QkPresenter
import org.groebl.sms.interactor.MarkUnblocked
import org.groebl.sms.repository.BlockingRepository
import org.groebl.sms.repository.ConversationRepository
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class BlockedRegexpsPresenter @Inject constructor(
    private val blockingRepo: BlockingRepository,
    private val conversationRepo: ConversationRepository,
    private val navigator: Navigator,
    private val markUnblocked: MarkUnblocked
) : QkPresenter<BlockedRegexpsView, BlockedRegexpsState>(
    BlockedRegexpsState(regexps = blockingRepo.getBlockedRegexps())
) {

    override fun bindIntents(view: BlockedRegexpsView) {
        super.bindIntents(view)

        view.unblockRegex()
            .doOnNext { id ->
                blockingRepo.getBlockedRegex(id)?.regex
                    ?.let(conversationRepo::getConversation)
                    ?.let { conversation -> markUnblocked.execute(listOf(conversation.id)) }
            }
            .doOnNext(blockingRepo::unblockRegex)
            .subscribeOn(Schedulers.io())
            .autoDisposable(view.scope())
            .subscribe()

        view.addRegex()
            .autoDisposable(view.scope())
            .subscribe { view.showAddDialog() }

        view.bannerRegexps()
            .autoDisposable(view.scope())
            .subscribe { navigator.showWikiRegexps() }

        view.saveRegex()
            .subscribeOn(Schedulers.io())
            .autoDisposable(view.scope())
            .subscribe { regex -> blockingRepo.blockRegex(regex) }
    }

}
