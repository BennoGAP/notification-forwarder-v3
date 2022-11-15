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

import android.content.res.ColorStateList
import android.view.View
import com.bluelinelabs.conductor.RouterTransaction
import com.jakewharton.rxbinding2.view.clicks
import org.groebl.sms.R
import org.groebl.sms.common.QkChangeHandler
import org.groebl.sms.common.base.QkController
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.extensions.animateLayoutChanges
import org.groebl.sms.common.util.extensions.resolveThemeColor
import org.groebl.sms.feature.blocking.manager.BlockingManagerController
import org.groebl.sms.feature.blocking.messages.BlockedMessagesController
import org.groebl.sms.feature.blocking.numbers.BlockedNumbersController
import org.groebl.sms.feature.blocking.regexps.BlockedRegexpsController
import org.groebl.sms.injection.appComponent
import org.groebl.sms.model.BlockedNumber
import org.groebl.sms.model.BlockedRegex
import org.groebl.sms.model.Conversation
import io.realm.OrderedRealmCollection
import io.realm.Realm
import kotlinx.android.synthetic.main.blocking_controller.*
import kotlinx.android.synthetic.main.settings_chevron_widget.view.*
import kotlinx.android.synthetic.main.settings_switch_widget.view.*
import javax.inject.Inject

class BlockingController : QkController<BlockingView, BlockingState, BlockingPresenter>(), BlockingView {

    override val blockingManagerIntent by lazy { blockingManager.clicks() }
    override val blockedNumbersIntent by lazy { blockedNumbers.clicks() }
    override val blockedMessagesIntent by lazy { blockedMessages.clicks() }
    override val blockedRegexpsIntent by lazy { blockedRegexps.clicks() }
    override val dropClickedIntent by lazy { drop.clicks() }

    @Inject lateinit var colors: Colors
    @Inject override lateinit var presenter: BlockingPresenter

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
        layoutRes = R.layout.blocking_controller
    }

    override fun onViewCreated() {
        super.onViewCreated()
        parent.postDelayed({ parent?.animateLayoutChanges = true }, 100)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.blocking_title)
        showBackButton(true)

        val states = arrayOf(
            intArrayOf(android.R.attr.state_activated),
            intArrayOf(-android.R.attr.state_activated))
        val textTertiary = view.context.resolveThemeColor(android.R.attr.textColorTertiary)
        val imageTintList = ColorStateList(states, intArrayOf(colors.theme().theme, textTertiary))

        blockedNumbers.chevron.imageTintList = imageTintList
        blockedRegexps.chevron.imageTintList = imageTintList
        blockedMessages.chevron.imageTintList = imageTintList
    }

    override fun render(state: BlockingState) {
        blockedNumbers.chevron.setImageResource(R.drawable.ic_chevron_right_black_24dp)
        blockedRegexps.chevron.setImageResource(R.drawable.ic_chevron_right_black_24dp)
        blockedMessages.chevron.setImageResource(R.drawable.ic_chevron_right_black_24dp)

        blockingManager.value = state.blockingManager
        drop.checkbox.isChecked = state.dropEnabled
        blockedMessages.isEnabled = !state.dropEnabled

        val blockedNumber: OrderedRealmCollection<BlockedNumber> = Realm.getDefaultInstance().where(BlockedNumber::class.java).findAll()
        blockedNumbers.value = if (state.blockingManager == activity!!.getString(R.string.blocking_manager_qksms_title_new)) blockedNumber.size.toString() else ""
        Realm.getDefaultInstance().close()

        val blockedRegexp: OrderedRealmCollection<BlockedRegex> = Realm.getDefaultInstance().where(BlockedRegex::class.java).findAll()
        blockedRegexps.value = if (state.blockingManager == activity!!.getString(R.string.blocking_manager_qksms_title_new)) blockedRegexp.size.toString() else ""
        Realm.getDefaultInstance().close()

        val blockedConversation: OrderedRealmCollection<Conversation> = Realm.getDefaultInstance().where(Conversation::class.java).equalTo("blocked", true).findAll()
        blockedMessages.value = blockedConversation.size.toString()
        Realm.getDefaultInstance().close()
    }

    override fun openBlockedNumbers() {
        router.pushController(RouterTransaction.with(BlockedNumbersController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

    override fun openBlockedRegexps() {
        router.pushController(RouterTransaction.with(BlockedRegexpsController())
            .pushChangeHandler(QkChangeHandler())
            .popChangeHandler(QkChangeHandler()))
    }

    override fun openBlockedMessages() {
        router.pushController(RouterTransaction.with(BlockedMessagesController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

    override fun openBlockingManager() {
        router.pushController(RouterTransaction.with(BlockingManagerController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

}
