package de.halfbit.comachine.dsl

import kotlin.reflect.KClass

@PublishedApi
internal data class OnEvent<State : Any, SubState : State, SubEvent : Any>(
    val eventType: KClass<SubEvent>,
    val eventDispatching: EventDispatching,
    val block: suspend OnEventBlock<State, SubState>.(SubEvent) -> Unit,
)

@PublishedApi
internal enum class EventDispatching {
    Sequential, Concurrent, Latest, Exclusive
}

@ComachineDsl
interface OnEventBlock<State : Any, SubState : State> {
    val state: SubState
    suspend fun SubState.update(block: SubState.() -> SubState)
    suspend fun transitionTo(block: SubState.() -> State): Nothing
    fun launch(block: suspend () -> Unit)
    fun launchInMachine(block: suspend () -> Unit)
}
