package org.groebl.sms.feature.blocking.manager

import org.groebl.sms.common.base.QkViewContract
import io.reactivex.Observable
import io.reactivex.Single

interface BlockingManagerView : QkViewContract<BlockingManagerState> {

    fun activityResumed(): Observable<*>
    fun qksmsClicked(): Observable<*>
    fun callBlockerClicked(): Observable<*>
    fun callControlClicked(): Observable<*>
    fun siaClicked(): Observable<*>

    fun showCopyDialog(manager: String): Single<Boolean>

}
