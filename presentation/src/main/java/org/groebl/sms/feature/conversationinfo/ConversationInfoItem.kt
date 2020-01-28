package org.groebl.sms.feature.conversationinfo

import org.groebl.sms.model.MmsPart
import org.groebl.sms.model.Recipient
import io.realm.RealmList

sealed class ConversationInfoItem {

    data class ConversationInfoRecipient(val value: Recipient) : ConversationInfoItem()

    data class ConversationInfoSettings(
        val name: String,
        val recipients: RealmList<Recipient>,
        val archived: Boolean,
        val blocked: Boolean
    ) : ConversationInfoItem()

    data class ConversationInfoMedia(val value: MmsPart) : ConversationInfoItem()

}
