package de.halfbit.comachine.runtime.dispatchers

import de.halfbit.comachine.dsl.LaunchBlock
import de.halfbit.comachine.runtime.EmitMessage
import de.halfbit.comachine.runtime.EventDispatcher
import de.halfbit.comachine.runtime.LaunchInState
import de.halfbit.comachine.runtime.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

internal class SingleEventDispatcher<State : Any, SubState : State, SubEvent : Any>(
    private val block: suspend LaunchBlock<State, SubState>.(SubEvent) -> Unit,
    private val launchBlock: LaunchBlock<State, SubState>,
    private val launchInStateFct: LaunchInState,
    private val emitMessage: EmitMessage,
) : EventDispatcher<SubEvent> {

    private var currentEventJob: Job? = null

    override fun onEventReceived(event: SubEvent) {
        currentEventJob?.let {
            if (it.isActive) return
        }
        currentEventJob = launchInStateFct {
            block(launchBlock, event)
            if (coroutineContext.isActive) {
                emitMessage(Message.OnEventCompleted(event))
            }
        }
    }

    override fun onEventCompleted(event: SubEvent) {
        currentEventJob = null
    }
}