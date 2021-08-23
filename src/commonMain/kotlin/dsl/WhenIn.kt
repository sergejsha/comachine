package de.halfbit.comachine.dsl

import kotlin.reflect.KClass

@PublishedApi
internal data class WhenIn<State : Any, SubState : State>(
    val stateType: KClass<SubState>,
    var onEnter: OnEnter<State, SubState>? = null,
    var onExit: OnExit<SubState>? = null,
    val onEvent: MutableMap<KClass<*>, OnEvent<State, SubState, *>> = mutableMapOf(),
)

@ComachineDsl
class WhenInBlock<State : Any, SubState : State, Event : Any>
@PublishedApi
internal constructor(
    @PublishedApi
    internal val whenIn: WhenIn<State, SubState>
) {

    /** A new event waits for the current event to complete. Multiple new events are buffered. */
    inline fun <reified SubEvent : Event> onSequential(
        noinline block: suspend OnEventBlock<State, SubState>.(SubEvent) -> Unit
    ) {
        addUnique(OnEvent(SubEvent::class, EventDispatching.Sequential, block))
    }

    /** A new event is executed concurrently to already processed events. */
    inline fun <reified SubEvent : Event> onConcurrent(
        noinline block: suspend OnEventBlock<State, SubState>.(SubEvent) -> Unit
    ) {
        addUnique(OnEvent(SubEvent::class, EventDispatching.Concurrent, block))
    }

    /** A new event replaces the current event, if such. Current event is cancelled. */
    inline fun <reified SubEvent : Event> onLatest(
        noinline block: suspend OnEventBlock<State, SubState>.(SubEvent) -> Unit
    ) {
        addUnique(OnEvent(SubEvent::class, EventDispatching.Latest, block))
    }

    /** A new event is ignored if there is the current event in processing. */
    inline fun <reified SubEvent : Event> onExclusive(
        noinline block: suspend OnEventBlock<State, SubState>.(SubEvent) -> Unit
    ) {
        addUnique(OnEvent(SubEvent::class, EventDispatching.Exclusive, block))
    }

    fun onEnter(block: OnEnterBlock<State, SubState>.() -> Unit) {
        whenIn.onEnter = OnEnter(block)
    }

    fun onExit(block: OnExitBlock<SubState>.() -> Unit) {
        whenIn.onExit = OnExit(block)
    }

    @PublishedApi
    internal fun addUnique(event: OnEvent<State, SubState, *>) {
        whenIn.onEvent.put(event.eventType, event)?.let {
            throw IllegalArgumentException(
                "Event handler for ${event.eventType} is already declared" +
                    " in ${whenIn.stateType}"
            )
        }
    }
}
