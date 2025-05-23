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
package org.groebl.sms.injection.android

import org.groebl.sms.feature.widget.WidgetProvider
import org.groebl.sms.injection.scope.ActivityScope
import org.groebl.sms.receiver.BlockThreadReceiver
import org.groebl.sms.receiver.BootReceiver
import org.groebl.sms.receiver.DefaultSmsChangedReceiver
import org.groebl.sms.receiver.DeleteMessagesReceiver
import org.groebl.sms.receiver.MarkArchivedReceiver
import org.groebl.sms.receiver.MarkReadReceiver
import org.groebl.sms.receiver.MarkSeenReceiver
import org.groebl.sms.receiver.MmsReceivedReceiver
import org.groebl.sms.receiver.MmsReceiver
import org.groebl.sms.receiver.MmsSentReceiver
import org.groebl.sms.receiver.MmsUpdatedReceiver
import org.groebl.sms.receiver.NightModeReceiver
import org.groebl.sms.receiver.RemoteMessagingReceiver
import org.groebl.sms.receiver.SendScheduledMessageReceiver
import org.groebl.sms.receiver.SmsDeliveredReceiver
import org.groebl.sms.receiver.SmsProviderChangedReceiver
import org.groebl.sms.receiver.SmsReceiver
import org.groebl.sms.receiver.SmsSentReceiver
import org.groebl.sms.feature.bluetooth.service.BluetoothBootReceiver
import org.groebl.sms.feature.bluetooth.service.BluetoothReceiver
import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.groebl.sms.feature.bluetooth.service.BluetoothNotificationService
import org.groebl.sms.receiver.SpeakThreadsReceiver
import org.groebl.sms.receiver.StartActivityFromWidgetReceiver

@Module
abstract class BroadcastReceiverBuilderModule {

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindBlockThreadReceiver(): BlockThreadReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindBootReceiver(): BootReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindDefaultSmsChangedReceiver(): DefaultSmsChangedReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindDeleteMessagesReceiver(): DeleteMessagesReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindMarkArchivedReceiver(): MarkArchivedReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindMarkReadReceiver(): MarkReadReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindSpeakThreadsReceiver(): SpeakThreadsReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindStartActivityFromWidgetReceiver(): StartActivityFromWidgetReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindMarkSeenReceiver(): MarkSeenReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindMmsReceivedReceiver(): MmsReceivedReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindMmsReceiver(): MmsReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindMmsSentReceiver(): MmsSentReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindMmsUpdatedReceiver(): MmsUpdatedReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindNightModeReceiver(): NightModeReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindRemoteMessagingReceiver(): RemoteMessagingReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindSendScheduledMessageReceiver(): SendScheduledMessageReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindSmsDeliveredReceiver(): SmsDeliveredReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindSmsProviderChangedReceiver(): SmsProviderChangedReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindSmsReceiver(): SmsReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindSmsSentReceiver(): SmsSentReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindWidgetProvider(): WidgetProvider

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindBluetoothBootReceiver(): BluetoothBootReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindBluetoothReceiver(): BluetoothReceiver

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindBluetoothNotification(): BluetoothNotificationService

}