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
package org.groebl.sms.common

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import androidx.core.provider.FontRequest
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import com.akaita.java.rxjava2debug.RxJava2Debug
import dagger.android.*
import io.realm.Realm
import io.realm.RealmConfiguration
import org.groebl.sms.R
import org.groebl.sms.common.util.CrashlyticsTree
import org.groebl.sms.common.util.FileLoggingTree
import org.groebl.sms.feature.bluetooth.common.BluetoothDatabase
import org.groebl.sms.injection.AppComponentManager
import org.groebl.sms.injection.appComponent
import org.groebl.sms.manager.AnalyticsManager
import org.groebl.sms.migration.QkRealmMigration
import org.groebl.sms.util.NightModeManager
import timber.log.Timber
import javax.inject.Inject



class QKApplication : Application(), HasActivityInjector, HasBroadcastReceiverInjector, HasServiceInjector {

    /**
     * Inject this so that it is forced to initialize
     */
    @Suppress("unused")
    @Inject lateinit var analyticsManager: AnalyticsManager
//    @Inject lateinit var mFirebaseAnalytics: FirebaseAnalytics


    @Inject lateinit var dispatchingActivityInjector: DispatchingAndroidInjector<Activity>
    @Inject lateinit var dispatchingBroadcastReceiverInjector: DispatchingAndroidInjector<BroadcastReceiver>
    @Inject lateinit var dispatchingServiceInjector: DispatchingAndroidInjector<Service>
    @Inject lateinit var fileLoggingTree: FileLoggingTree
    @Inject lateinit var nightModeManager: NightModeManager

    override fun onCreate() {
        super.onCreate()

//        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        Realm.init(this)
        Realm.setDefaultConfiguration(RealmConfiguration.Builder()
                .compactOnLaunch()
                .migration(QkRealmMigration())
                .schemaVersion(QkRealmMigration.SCHEMA_VERSION)
                .build())

        AppComponentManager.init(this)
        appComponent.inject(this)

        //packageManager.getInstallerPackageName(packageName)?.let { installer ->
        //    analyticsManager.setUserProperty("Installer", installer)
        //}

        nightModeManager.updateCurrentTheme()

        val fontRequest = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs)

        EmojiCompat.init(FontRequestEmojiCompatConfig(this, fontRequest))

        Timber.plant(Timber.DebugTree(), CrashlyticsTree(), fileLoggingTree)

        RxJava2Debug.enableRxJava2AssemblyTracking(arrayOf("org.groebl.sms"))

        BluetoothDatabase.init(this)
    }

    override fun activityInjector(): AndroidInjector<Activity> {
        return dispatchingActivityInjector
    }

    override fun broadcastReceiverInjector(): AndroidInjector<BroadcastReceiver> {
        return dispatchingBroadcastReceiverInjector
    }

    override fun serviceInjector(): AndroidInjector<Service> {
        return dispatchingServiceInjector
    }

}