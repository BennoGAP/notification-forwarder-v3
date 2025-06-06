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
package org.groebl.sms.repository

import org.groebl.sms.model.ScheduledMessage
import io.realm.RealmResults

interface ScheduledMessageRepository {

    /**
     * Saves a scheduled message
     */
    fun saveScheduledMessage(
        date: Long,
        subId: Int,
        recipients: List<String>,
        sendAsGroup: Boolean,
        body: String,
        attachments: List<String>
    ): ScheduledMessage

    fun updateScheduledMessage(scheduledMessage: ScheduledMessage)

    /**
     * Returns all of the scheduled messages, sorted chronologically
     */
    fun getScheduledMessages(): RealmResults<ScheduledMessage>

    /**
     * Returns the scheduled message with the given [id]
     */
    fun getScheduledMessage(id: Long): ScheduledMessage?

    /**
     * Deletes the scheduled message with the given [id]
     */
    fun deleteScheduledMessage(id: Long)

    // delete multiple scheduled messages by id list
    fun deleteScheduledMessages(ids: List<Long>)

    // get a list of all scheduled message ids (in scheduled date order)
    fun getAllScheduledMessageIdsSnapshot(): List<Long>

}