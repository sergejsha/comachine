package de.halfbit.comachine.dsl

import kotlinx.coroutines.Job

@PublishedApi
internal data class OnEnter<State : Any, SubState : State>(
    val block: OnEnterBlock<State, SubState>.() -> Unit,
)

@ComachineDsl
interface OnEnterBlock<State : Any, SubState : State> {
    val state: SubState
    suspend fun SubState.update(block: SubState.() -> SubState)
    suspend fun transitionTo(block: SubState.() -> State): Nothing
    fun launch(block: suspend () -> Unit): Job
    fun launchInMachine(block: suspend () -> Unit)
}
