package de.halfbit.comachine.dsl

@PublishedApi
internal data class OnEnter<State : Any, SubState : State>(
    val block: OnEventBlock<State, SubState>.() -> Unit,
    val mainBlock: Boolean = false,
    override var next: OnEnter<State, SubState>? = null,
) : Linked<OnEnter<State, SubState>>
