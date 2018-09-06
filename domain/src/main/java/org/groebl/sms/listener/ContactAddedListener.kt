package org.groebl.sms.listener

import io.reactivex.Single

interface ContactAddedListener {

    fun listen(address: String): Single<*>

}