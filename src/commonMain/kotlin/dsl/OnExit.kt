package de.halfbit.comachine.dsl

@PublishedApi
internal data class OnExit<SubState : Any>(
    val block: OnExitBlock<SubState>.() -> Unit,
    val mainBlock: Boolean = false,
    override var next: OnExit<SubState>? = null,
) : Linked<OnExit<SubState>>
