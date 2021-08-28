package de.halfbit.comachine.dsl

import kotlin.reflect.KClass

@PublishedApi
internal data class OnEvent<State : Any, SubState : State, SubEvent : Any>(
    val eventType: KClass<SubEvent>,
    val eventDispatching: EventDispatching,
    val block: suspend LaunchBlock<State, SubState>.(SubEvent) -> Unit,
)

@PublishedApi
internal enum class EventDispatching {
    Sequential, Concurrent, Latest, Exclusive
}
