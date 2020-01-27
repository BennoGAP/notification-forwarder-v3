/*
 * Copyright (C) 2019 Moez Bhatti <moez.bhatti@gmail.com>
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
package org.groebl.sms.feature.contacts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.jakewharton.rxbinding2.widget.editorActions
import com.jakewharton.rxbinding2.widget.textChanges
import org.groebl.sms.R
import org.groebl.sms.common.ViewModelFactory
import org.groebl.sms.common.base.QkThemedActivity
import org.groebl.sms.common.widget.QkDialog
import org.groebl.sms.extensions.Optional
import org.groebl.sms.feature.compose.editing.ComposeItem
import org.groebl.sms.feature.compose.editing.ComposeItemAdapter
import org.groebl.sms.feature.compose.editing.PhoneNumberAction
import org.groebl.sms.feature.compose.editing.PhoneNumberPickerAdapter
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.contacts_activity.*
import javax.inject.Inject

class ContactsActivity : QkThemedActivity(), ContactsContract {

    companion object {
        const val ChipsKey = "chips"
    }

    @Inject lateinit var contactsAdapter: ComposeItemAdapter
    @Inject lateinit var phoneNumberAdapter: PhoneNumberPickerAdapter
    @Inject lateinit var viewModelFactory: ViewModelFactory

    override val queryChangedIntent: Observable<CharSequence> by lazy { search.textChanges() }
    override val queryBackspaceIntent: Observable<*> by lazy { search.backspaces }
    override val queryEditorActionIntent: Observable<Int> by lazy { search.editorActions() }
    override val composeItemPressedIntent: Subject<ComposeItem> by lazy { contactsAdapter.clicks }
    override val composeItemLongPressedIntent: Subject<ComposeItem> by lazy { contactsAdapter.longClicks }
    override val phoneNumberSelectedIntent: Subject<Optional<Long>> by lazy { phoneNumberAdapter.selectedItemChanges }
    override val phoneNumberActionIntent: Subject<PhoneNumberAction> = PublishSubject.create()

    private val viewModel by lazy { ViewModelProviders.of(this, viewModelFactory)[ContactsViewModel::class.java] }

    private val phoneNumberDialog by lazy {
        QkDialog(this).apply {
            titleRes = R.string.compose_number_picker_title
            adapter = phoneNumberAdapter
            positiveButton = R.string.compose_number_picker_always
            positiveButtonListener = { phoneNumberActionIntent.onNext(PhoneNumberAction.ALWAYS) }
            negativeButton = R.string.compose_number_picker_once
            negativeButtonListener = { phoneNumberActionIntent.onNext(PhoneNumberAction.JUST_ONCE) }
            cancelListener = { phoneNumberActionIntent.onNext(PhoneNumberAction.CANCEL) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contacts_activity)
        showBackButton(true)
        viewModel.bindView(this)

        contacts.itemAnimator = null
        contacts.adapter = contactsAdapter
    }

    override fun render(state: ContactsState) {
        contactsAdapter.data = state.composeItems

        if (state.selectedContact != null && !phoneNumberDialog.isShowing) {
            phoneNumberAdapter.data = state.selectedContact.numbers
            phoneNumberDialog.subtitle = state.selectedContact.name
            phoneNumberDialog.show()
        } else if (state.selectedContact == null && phoneNumberDialog.isShowing) {
            phoneNumberDialog.dismiss()
        }
    }

    override fun finish(result: HashMap<String, String?>) {
        val intent = Intent().putExtra(ChipsKey, result)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

}