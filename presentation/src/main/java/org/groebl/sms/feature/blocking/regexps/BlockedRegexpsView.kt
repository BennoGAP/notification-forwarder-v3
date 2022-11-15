package org.groebl.sms.feature.blocking.regexps

import org.groebl.sms.common.base.QkViewContract
import io.reactivex.Observable

interface BlockedRegexpsView : QkViewContract<BlockedRegexpsState> {

    fun unblockRegex(): Observable<Long>
    fun addRegex(): Observable<*>
    fun bannerRegexps(): Observable<*>
    fun saveRegex(): Observable<String>

    fun showAddDialog()

}