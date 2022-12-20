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
package org.groebl.sms.feature.backup

import android.content.Context
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.groebl.sms.R
import org.groebl.sms.common.base.QkPresenter
import org.groebl.sms.common.util.DateFormatter
import org.groebl.sms.common.util.extensions.makeToast
import org.groebl.sms.interactor.PerformBackup
import org.groebl.sms.manager.PermissionManager
import org.groebl.sms.repository.BackupRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class BackupPresenter @Inject constructor(
        private val backupRepo: BackupRepository,
        private val context: Context,
        private val dateFormatter: DateFormatter,
        private val performBackup: PerformBackup,
        private val permissionManager: PermissionManager
) : QkPresenter<BackupView, BackupState>(BackupState()) {

    private val storagePermissionSubject: Subject<Boolean> = BehaviorSubject.createDefault(permissionManager.hasStorage())

    init {
        disposables += backupRepo.getBackupProgress()
                .sample(16, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .subscribe { progress -> newState { copy(backupProgress = progress) } }

        disposables += backupRepo.getRestoreProgress()
                .sample(16, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .subscribe { progress -> newState { copy(restoreProgress = progress) } }

        disposables += storagePermissionSubject
                .distinctUntilChanged()
                .switchMap { backupRepo.getBackups() }
                .doOnNext { backups -> newState { copy(backups = backups) } }
                .map { backups -> backups.maxOfOrNull { it.date } ?: 0L }
                .map { lastBackup ->
                    when (lastBackup) {
                        0L -> context.getString(R.string.backup_never)
                        else -> dateFormatter.getDetailedTimestamp(lastBackup)
                    }
                }
                .startWith(context.getString(R.string.backup_loading))
                .subscribe { lastBackup -> newState { copy(lastBackup = lastBackup) } }
    }

    override fun bindIntents(view: BackupView) {
        super.bindIntents(view)

        view.activityVisible()
                .map { permissionManager.hasStorage() }
                .autoDisposable(view.scope())
                .subscribe(storagePermissionSubject)

        view.restoreClicks()
                .withLatestFrom(
                        backupRepo.getBackupProgress(),
                        backupRepo.getRestoreProgress())
                { _, backupProgress, restoreProgress ->
                    when {
                        backupProgress.running -> context.makeToast(R.string.backup_restore_error_backup)
                        restoreProgress.running -> context.makeToast(R.string.backup_restore_error_restore)
                        !permissionManager.hasStorage() -> view.requestStoragePermission()
                        else -> view.selectFile()
                    }
                }
                .autoDisposable(view.scope())
                .subscribe()

        view.restoreFileSelected()
                .autoDisposable(view.scope())
                .subscribe { view.confirmRestore() }

        view.restoreConfirmed()
                .withLatestFrom(view.restoreFileSelected()) { _, backup -> backup }
                .autoDisposable(view.scope())
                .subscribe { backup -> RestoreBackupService.start(context, backup.path) }

        view.stopRestoreClicks()
                .autoDisposable(view.scope())
                .subscribe { view.stopRestore() }

        view.stopRestoreConfirmed()
                .autoDisposable(view.scope())
                .subscribe { backupRepo.stopRestore() }

        view.fabClicks()
                .autoDisposable(view.scope())
                .subscribe {
                    when {
                        !permissionManager.hasStorage() -> view.requestStoragePermission()
                        else -> performBackup.execute(Unit)
                    }
                }
    }

}