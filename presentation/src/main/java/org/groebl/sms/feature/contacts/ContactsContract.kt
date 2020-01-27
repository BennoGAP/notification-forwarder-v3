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

import io.reactivex.Observable
import io.reactivex.subjects.Subject
import org.groebl.sms.common.base.QkView
import org.groebl.sms.extensions.Optional
import org.groebl.sms.feature.compose.editing.ComposeItem
import org.groebl.sms.feature.compose.editing.PhoneNumberAction

interface ContactsContract : QkView<ContactsState> {

    val queryChangedIntent: Observable<CharSequence>
    val queryBackspaceIntent: Observable<*>
    val queryEditorActionIntent: Observable<Int>
    val composeItemPressedIntent: Subject<ComposeItem>
    val composeItemLongPressedIntent: Subject<ComposeItem>
    val phoneNumberSelectedIntent: Subject<Optional<Long>>
    val phoneNumberActionIntent: Subject<PhoneNumberAction>

    fun openKeyboard()
    fun finish(result: HashMap<String, String?>)

}