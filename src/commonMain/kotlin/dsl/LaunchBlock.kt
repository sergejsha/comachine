package de.halfbit.comachine.dsl

import de.halfbit.comachine.runtime.TransitionPerformedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@ComachineDsl
class LaunchBlock<State : Any, SubState : State>
internal constructor(
    private val getStateFct: () -> SubState,
    private val stateScope: CoroutineScope,
    private val machineScope: CoroutineScope,
    private val updateStateFct: suspend ((SubState) -> SubState) -> Unit,
    private val transitionToFct: suspend ((SubState) -> State) -> Unit,
) {
    val state: SubState get() = getStateFct()

    suspend fun SubState.update(block: SubState.() -> SubState) {
        updateStateFct(block)
    }

    suspend fun transitionTo(block: SubState.() -> State): Nothing {
        transitionToFct(block)
        throw TransitionPerformedException()
    }

    fun launch(block: LaunchBlockReceiver<State, SubState>): Job =
        stateScope.launch { block(this@LaunchBlock) }

    fun launchInMachine(block: LaunchBlockReceiver<State, SubState>): Job =
        machineScope.launch { block(this@LaunchBlock) }
}

internal typealias LaunchBlockReceiver<State, SubState> =
    suspend LaunchBlock<State, SubState>.() -> Unit
