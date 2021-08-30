package de.halfbit.comachine.dsl

import kotlin.reflect.KClass

@PublishedApi
internal sealed interface OnEvent<State : Any, SubState : State, SubEvent : Any> {
    val eventType: KClass<SubEvent>

    class NonSuspendable<State : Any, SubState : State, SubEvent : Any>(
        override val eventType: KClass<SubEvent>,
        val block: OnEventBlock<State, SubState>.(SubEvent) -> Unit,
        val next: NonSuspendable<State, SubState, SubEvent>?,
    ) : OnEvent<State, SubState, SubEvent>

    class Suspendable<State : Any, SubState : State, SubEvent : Any>(
        override val eventType: KClass<SubEvent>,
        val launchMode: LaunchMode,
        val block: suspend LaunchBlock<State, SubState>.(SubEvent) -> Unit,
    ) : OnEvent<State, SubState, SubEvent>
}

@PublishedApi
internal enum class LaunchMode {
    Sequential, Concurrent, Latest, Single
}
