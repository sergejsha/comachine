package de.halfbit.comachine.dsl

import kotlinx.coroutines.Job

@PublishedApi
internal data class OnEnter<State : Any, SubState : State>(
    val block: OnEventBlock<State, SubState>.() -> Unit,
)
