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
    internal val whenIn: WhenIn<State, SubState>
) {

    /** A new event waits for the current event to complete. Multiple new events are buffered. */
    inline fun <reified SubEvent : Event> onSequential(
        noinline block: suspend LaunchBlock<State, SubState>.(SubEvent) -> Unit
    ) {
        addUniqueEvent(OnEvent.Launchable(SubEvent::class, LaunchMode.Sequential, block))
    }

    /** A new event is executed concurrently to already processed events. */
    inline fun <reified SubEvent : Event> onConcurrent(
        noinline block: suspend LaunchBlock<State, SubState>.(SubEvent) -> Unit
    ) {
        addUniqueEvent(OnEvent.Launchable(SubEvent::class, LaunchMode.Concurrent, block))
    }

    /** A new event replaces the current event, if such. Current event is cancelled. */
    inline fun <reified SubEvent : Event> onLatest(
        noinline block: suspend LaunchBlock<State, SubState>.(SubEvent) -> Unit
    ) {
        addUniqueEvent(OnEvent.Launchable(SubEvent::class, LaunchMode.Latest, block))
    }

    /** A new event is ignored if there is an event in processing. */
    inline fun <reified SubEvent : Event> onSingle(
        noinline block: suspend LaunchBlock<State, SubState>.(SubEvent) -> Unit
    ) {
        addUniqueEvent(OnEvent.Launchable(SubEvent::class, LaunchMode.Single, block))
    }

    inline fun <reified SubEvent : Event> on(
        noinline block: OnEventBlock<State, SubState>.(SubEvent) -> Unit,
    ) {
        addUniqueEvent(OnEvent.Default(SubEvent::class, block))
    }

    fun onEnter(block: OnEventBlock<State, SubState>.() -> Unit) {
        whenIn.onEnter = OnEnter(block)
    }

    fun onExit(block: OnExitBlock<SubState>.() -> Unit) {
        whenIn.onExit = OnExit(block)
    }

    @PublishedApi
    internal fun addUniqueEvent(event: OnEvent<State, SubState, *>) {
        whenIn.onEvent.put(event.eventType, event)?.let {
            throw IllegalArgumentException(
                "Event handler for ${event.eventType} is already declared" +
                    " in ${whenIn.stateType}"
            )
        }
    }
}
