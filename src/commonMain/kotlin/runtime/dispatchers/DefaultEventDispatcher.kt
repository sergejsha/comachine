package de.halfbit.comachine.runtime.dispatchers

import de.halfbit.comachine.runtime.EventDispatcher
import de.halfbit.comachine.dsl.OnEventBlock

internal class DefaultEventDispatcher<State : Any, SubState : State, SubEvent : Any>(
    private val eventRuntime: OnEventBlock<State, SubState>,
    private val block: OnEventBlock<State, SubState>.(SubEvent) -> Unit,
) : EventDispatcher<SubEvent> {

    override fun onEventReceived(event: SubEvent) {
        block(eventRuntime, event)
    }
}
