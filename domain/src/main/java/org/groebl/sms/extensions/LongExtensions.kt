package org.groebl.sms.extensions

import java.util.concurrent.TimeUnit

fun Long.millisecondsToMinutes(): Long {
    return TimeUnit.MILLISECONDS.toMinutes(this)
}
