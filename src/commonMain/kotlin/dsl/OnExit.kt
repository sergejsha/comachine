package de.halfbit.comachine.dsl

@PublishedApi
internal data class OnExit<SubState : Any>(
    val block: OnExitBlock<SubState>.() -> Unit,
)

@ComachineDsl
interface OnExitBlock<SubState : Any> {
    val state: SubState
    fun launchInMachine(block: suspend () -> Unit)
}
