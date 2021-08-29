package de.halfbit.comachine.dsl

@PublishedApi
internal data class OnEnter<State : Any, SubState : State>(
    val block: OnEventBlock<State, SubState>.() -> Unit,
)
