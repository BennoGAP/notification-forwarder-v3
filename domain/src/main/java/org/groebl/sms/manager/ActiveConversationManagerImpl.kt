package org.groebl.smsmanager

import org.groebl.sms.manager.ActiveConversationManager
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class ActiveConversationManagerImpl @Inject constructor() : ActiveConversationManager {
    private var threadId: Long? = null
    override fun setActiveConversation(threadId: Long?) {
        this.threadId = threadId
    }
    override fun getActiveConversation(): Long? {
        return threadId
    }
}