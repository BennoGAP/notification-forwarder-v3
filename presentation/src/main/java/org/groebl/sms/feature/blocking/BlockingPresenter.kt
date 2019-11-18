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
package org.groebl.sms.feature.blocking

import android.content.Context
import org.groebl.sms.R
import org.groebl.sms.blocking.BlockingClient
import org.groebl.sms.common.base.QkPresenter
import org.groebl.sms.util.Preferences
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.rxkotlin.plusAssign
import javax.inject.Inject

class BlockingPresenter @Inject constructor(
    context: Context,
    private val blockingClient: BlockingClient,
    private val prefs: Preferences
) : QkPresenter<BlockingView, BlockingState>(BlockingState()) {

    init {
        disposables += prefs.blockingManager.asObservable()
                .map { client ->
                    when (client) {
                        Preferences.BLOCKING_MANAGER_SIA -> R.string.blocking_manager_sia_title
                        Preferences.BLOCKING_MANAGER_CC -> R.string.blocking_manager_call_control_title
                        else -> R.string.blocking_manager_qksms_title_new
                    }
                }
                .map(context::getString)
                .subscribe { manager -> newState { copy(blockingManager = manager) } }

        disposables += prefs.drop.asObservable()
                .subscribe { enabled -> newState { copy(dropEnabled = enabled) } }
    }

    override fun bindIntents(view: BlockingView) {
        super.bindIntents(view)

        view.blockingManagerIntent
                .autoDisposable(view.scope())
                .subscribe { view.openBlockingManager() }

        view.blockedNumbersIntent
                .autoDisposable(view.scope())
                .subscribe {
                    if (prefs.blockingManager.get() == Preferences.BLOCKING_MANAGER_QKSMS) {
                        // TODO: This is a hack, get rid of it once we implement AndroidX navigation
                        view.openBlockedNumbers()
                    } else {
                        blockingClient.openSettings()
                    }
                }

        view.blockedMessagesIntent
                .autoDisposable(view.scope())
                .subscribe { view.openBlockedMessages() }

        view.dropClickedIntent
                .autoDisposable(view.scope())
                .subscribe { prefs.drop.set(!prefs.drop.get()) }
    }

}
