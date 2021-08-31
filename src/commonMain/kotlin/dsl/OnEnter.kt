package de.halfbit.comachine.dsl

@PublishedApi
internal data class OnEnter<State : Any, SubState : State>(
    val mainBlock: Boolean = false,
    val block: OnEventBlock<State, SubState>.() -> Unit,
    var next: OnEnter<State, SubState>? = null,
)
