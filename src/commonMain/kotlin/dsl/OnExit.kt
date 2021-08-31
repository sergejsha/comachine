package de.halfbit.comachine.dsl

@PublishedApi
internal data class OnExit<SubState : Any>(
    val mainBlock: Boolean = false,
    val block: OnExitBlock<SubState>.() -> Unit,
    var next: OnExit<SubState>? = null,
)
