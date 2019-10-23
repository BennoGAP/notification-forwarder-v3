package org.groebl.sms.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class BlockedNumber(
        @PrimaryKey var id: Long = 0,

        var address: String = ""
) : RealmObject()