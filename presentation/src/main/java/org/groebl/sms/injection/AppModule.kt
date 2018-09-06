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
package org.groebl.sms.injection

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.preference.PreferenceManager
import androidx.lifecycle.ViewModelProvider
import com.f2prateek.rx.preferences2.RxSharedPreferences
import org.groebl.sms.common.ViewModelFactory
import org.groebl.sms.common.util.NotificationManagerImpl
import org.groebl.sms.common.util.ShortcutManagerImpl
import org.groebl.sms.feature.conversationinfo.injection.ConversationInfoComponent
import org.groebl.sms.feature.themepicker.injection.ThemePickerComponent
import org.groebl.sms.listener.ContactAddedListener
import org.groebl.sms.listener.ContactAddedListenerImpl
import org.groebl.sms.manager.AlarmManager
import org.groebl.sms.manager.AlarmManagerImpl
import org.groebl.sms.manager.AnalyticsManager
import org.groebl.sms.manager.AnalyticsManagerImpl
import org.groebl.sms.manager.ExternalBlockingManager
import org.groebl.sms.manager.ExternalBlockingManagerImpl
import org.groebl.sms.manager.KeyManager
import org.groebl.sms.manager.KeyManagerImpl
import org.groebl.sms.manager.NotificationManager
import org.groebl.sms.manager.PermissionManager
import org.groebl.sms.manager.PermissionManagerImpl
import org.groebl.sms.manager.RatingManager
import org.groebl.sms.manager.ShortcutManager
import org.groebl.sms.manager.WidgetManager
import org.groebl.sms.manager.WidgetManagerImpl
import org.groebl.sms.mapper.CursorToContact
import org.groebl.sms.mapper.CursorToContactImpl
import org.groebl.sms.mapper.CursorToConversation
import org.groebl.sms.mapper.CursorToConversationImpl
import org.groebl.sms.mapper.CursorToMessage
import org.groebl.sms.mapper.CursorToMessageImpl
import org.groebl.sms.mapper.CursorToPart
import org.groebl.sms.mapper.CursorToPartImpl
import org.groebl.sms.mapper.CursorToRecipient
import org.groebl.sms.mapper.CursorToRecipientImpl
import org.groebl.sms.mapper.RatingManagerImpl
import org.groebl.sms.repository.BackupRepository
import org.groebl.sms.repository.BackupRepositoryImpl
import org.groebl.sms.repository.ContactRepository
import org.groebl.sms.repository.ContactRepositoryImpl
import org.groebl.sms.repository.ConversationRepository
import org.groebl.sms.repository.ConversationRepositoryImpl
import org.groebl.sms.repository.ImageRepository
import org.groebl.sms.repository.ImageRepostoryImpl
import org.groebl.sms.repository.MessageRepository
import org.groebl.sms.repository.MessageRepositoryImpl
import org.groebl.sms.repository.ScheduledMessageRepository
import org.groebl.sms.repository.ScheduledMessageRepositoryImpl
import org.groebl.sms.repository.SyncRepository
import org.groebl.sms.repository.SyncRepositoryImpl
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(subcomponents = [
    ConversationInfoComponent::class,
    ThemePickerComponent::class])
class AppModule(private var application: Application) {

    @Provides
    @Singleton
    fun provideContext(): Context = application

    @Provides
    fun provideContentResolver(context: Context): ContentResolver = context.contentResolver

    @Provides
    @Singleton
    fun provideRxPreferences(context: Context): RxSharedPreferences {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return RxSharedPreferences.create(preferences)
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder().build()
    }

    @Provides
    fun provideViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory = factory

    // Listener

    @Provides
    fun provideContactAddedListener(listener: ContactAddedListenerImpl): ContactAddedListener = listener

    // Manager

    @Provides
    fun provideAlarmManager(manager: AlarmManagerImpl): AlarmManager = manager

    @Provides
    fun provideAnalyticsManager(manager: AnalyticsManagerImpl): AnalyticsManager = manager

    @Provides
    fun externalBlockingManager(manager: ExternalBlockingManagerImpl): ExternalBlockingManager = manager

    @Provides
    fun provideKeyManager(manager: KeyManagerImpl): KeyManager = manager

    @Provides
    fun provideNotificationsManager(manager: NotificationManagerImpl): NotificationManager = manager

    @Provides
    fun providePermissionsManager(manager: PermissionManagerImpl): PermissionManager = manager

    @Provides
    fun provideRatingManager(manager: RatingManagerImpl): RatingManager = manager

    @Provides
    fun provideShortcutManager(manager: ShortcutManagerImpl): ShortcutManager = manager

    @Provides
    fun provideWidgetManager(manager: WidgetManagerImpl): WidgetManager = manager


    // Mapper

    @Provides
    fun provideCursorToContact(mapper: CursorToContactImpl): CursorToContact = mapper

    @Provides
    fun provideCursorToConversation(mapper: CursorToConversationImpl): CursorToConversation = mapper

    @Provides
    fun provideCursorToMessage(mapper: CursorToMessageImpl): CursorToMessage = mapper

    @Provides
    fun provideCursorToPart(mapper: CursorToPartImpl): CursorToPart = mapper

    @Provides
    fun provideCursorToRecipient(mapper: CursorToRecipientImpl): CursorToRecipient = mapper


    // Repository

    @Provides
    fun provideBackupRepository(repository: BackupRepositoryImpl): BackupRepository = repository

    @Provides
    fun provideContactRepository(repository: ContactRepositoryImpl): ContactRepository = repository

    @Provides
    fun provideConversationRepository(repository: ConversationRepositoryImpl): ConversationRepository = repository

    @Provides
    fun provideImageRepository(repository: ImageRepostoryImpl): ImageRepository = repository

    @Provides
    fun provideMessageRepository(repository: MessageRepositoryImpl): MessageRepository = repository

    @Provides
    fun provideScheduledMessagesRepository(repository: ScheduledMessageRepositoryImpl): ScheduledMessageRepository = repository

    @Provides
    fun provideSyncRepository(repository: SyncRepositoryImpl): SyncRepository = repository

}