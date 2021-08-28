package de.halfbit.comachine.runtime.dispatchers

import de.halfbit.comachine.dsl.LaunchBlock
import de.halfbit.comachine.dsl.OnEvent
import de.halfbit.comachine.runtime.EmitMessage
import de.halfbit.comachine.runtime.LaunchInState
import de.halfbit.comachine.runtime.Message
import de.halfbit.comachine.runtime.OnEventDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

internal class SequentialEventDispatcher<State : Any, SubState : State, SubEvent : Any>(
    private val onEvent: OnEvent<State, SubState, SubEvent>,
    private val launchInStateFct: LaunchInState,
    private val emitMessage: EmitMessage,
    private val launchBlock: LaunchBlock<State, SubState>,
) : OnEventDispatcher<SubEvent> {

    private val queuedEvents: MutableList<SubEvent> = mutableListOf()
    private var currentEventJob: Job? = null

    override fun onEventReceived(event: SubEvent) {
        if (currentEventJob != null) queuedEvents += event
        else currentEventJob = launchNextEventJob(event)
    }

    override fun onEventCompleted(event: SubEvent) {
        currentEventJob =
            if (queuedEvents.isEmpty()) null
            else launchNextEventJob(queuedEvents.removeFirst())
    }

    private fun launchNextEventJob(event: SubEvent): Job =
        launchInStateFct {
            onEvent.block(launchBlock, event)
            if (coroutineContext.isActive) {
                emitMessage(Message.OnEventCompleted(event))
            }
        }
}