package org.groebl.sms.feature.blocking.regexps

import org.groebl.sms.model.BlockedRegex
import io.realm.RealmResults

data class BlockedRegexpsState(
    val regexps: RealmResults<BlockedRegex>? = null
)
