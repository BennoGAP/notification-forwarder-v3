package org.groebl.sms.listener

import io.reactivex.Observable

interface ContactAddedListener {

    fun listen(address: String): Observable<*>

}