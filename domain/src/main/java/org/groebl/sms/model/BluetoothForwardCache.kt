package org.groebl.sms.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class BluetoothForwardCache(
        @PrimaryKey var id: Long = 0,
        var app: String = "",
        var hash: String = "",
        var date: Long = System.currentTimeMillis()
) : RealmObject()
