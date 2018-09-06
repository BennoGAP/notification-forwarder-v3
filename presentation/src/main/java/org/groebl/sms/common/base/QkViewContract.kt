package org.groebl.sms.common.base

import com.bluelinelabs.conductor.autodispose.ControllerEvent
import com.uber.autodispose.LifecycleScopeProvider

interface QkViewContract<in State> {

    fun render(state: State)

    fun scope(): LifecycleScopeProvider<ControllerEvent>

}