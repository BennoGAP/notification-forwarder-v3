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
package org.groebl.sms.feature.main

import io.realm.RealmResults
import org.groebl.sms.model.Conversation
import org.groebl.sms.model.SearchResult
import org.groebl.sms.repository.SyncRepository

data class MainState(
        val hasError: Boolean = false,
        val page: MainPage = Inbox(),
        val drawerOpen: Boolean = false,
        val showRating: Boolean = false,
        val syncing: SyncRepository.SyncProgress = SyncRepository.SyncProgress.Idle(),
        val defaultSms: Boolean = false,
        val smsPermission: Boolean = false,
        val contactPermission: Boolean = false
)

sealed class MainPage

data class Inbox(
        val markPinned: Boolean = true,
        val markRead: Boolean = false,
        val data: RealmResults<Conversation>? = null,
        val selected: Int = 0) : MainPage()

data class Searching(
        val loading: Boolean = false,
        val data: List<SearchResult>? = null
) : MainPage()

data class Archived(
        val markPinned: Boolean = true,
        val markRead: Boolean = false,
        val data: RealmResults<Conversation>? = null,
        val selected: Int = 0) : MainPage()