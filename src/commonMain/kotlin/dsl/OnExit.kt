package de.halfbit.comachine.dsl

@PublishedApi
internal data class OnExit<SubState : Any>(
    val block: OnExitBlock<SubState>.() -> Unit,
    val next: OnExit<SubState>?,
)
