package org.groebl.sms.service

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import androidx.core.content.getSystemService
import org.groebl.sms.interactor.DeleteOldMessages
import dagger.android.AndroidInjection
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.Job
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AutoDeleteService : JobService() {

    companion object {
        private lateinit var jobScheduler: JobScheduler
        private const val JobId = 8120235

        @SuppressLint("MissingPermission") // Added in [presentation]'s AndroidManifest.xml
        fun scheduleJob(context: Context) {
            Timber.i("Scheduling job")
            val serviceComponent = ComponentName(context, AutoDeleteService::class.java)
            val periodicJob = JobInfo.Builder(JobId, serviceComponent)
                    .setPeriodic(TimeUnit.DAYS.toMillis(1))
                    .setPersisted(true)
                    .build()

            jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(periodicJob)
        }

        fun cancelJob(context: Context) {
            Timber.i("Canceling job")
            jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(JobId)
        }
    }

    @Inject lateinit var deleteOldMessages: DeleteOldMessages

    private val disposables = CompositeDisposable()

    override fun onStartJob(params: JobParameters?): Boolean {
        Timber.i("onStartJob")
        AndroidInjection.inject(this)
        disposables += deleteOldMessages
        deleteOldMessages.execute(Unit) {
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Timber.i("onStopJob")
        disposables.dispose()
        return true
    }
}
