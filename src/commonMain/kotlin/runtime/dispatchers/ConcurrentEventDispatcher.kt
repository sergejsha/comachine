package de.halfbit.comachine.runtime.dispatchers

import de.halfbit.comachine.dsl.OnEvent
import de.halfbit.comachine.runtime.EmitMessage
import de.halfbit.comachine.runtime.LaunchInState
import de.halfbit.comachine.runtime.Message
import de.halfbit.comachine.runtime.OnEventDispatcher
import de.halfbit.comachine.runtime.OnEventRuntime
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

internal class ConcurrentEventDispatcher<State : Any, SubState : State, SubEvent : Any>(
    private val onEvent: OnEvent<State, SubState, SubEvent>,
    private val launchInStateFct: LaunchInState,
    private val emitMessage: EmitMessage,
    private val onEventRuntime: OnEventRuntime<State, SubState>,
) : OnEventDispatcher<SubEvent> {

    override fun onEventReceived(event: SubEvent) {
        launchInStateFct {
            onEvent.block(onEventRuntime, event)
            if (coroutineContext.isActive) {
                emitMessage(Message.OnEventCompleted(event))
            }
        }
    }

    override fun onEventCompleted(event: SubEvent) {
        // do nothing
    }
}