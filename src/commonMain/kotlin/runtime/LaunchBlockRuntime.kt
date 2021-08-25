package de.halfbit.comachine.runtime

import de.halfbit.comachine.dsl.LaunchBlock
import de.halfbit.comachine.dsl.LaunchBlockReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

internal class LaunchBlockRuntime<State : Any, SubState : State>(
    private val getStateFct: () -> SubState,
    private val stateScope: CoroutineScope,
    private val machineScope: CoroutineScope,
    private val updateStateFct: suspend ((SubState) -> SubState) -> Unit,
    private val transitionToFct: suspend ((SubState) -> State) -> Unit,
) : LaunchBlock<State, SubState> {

    override val state: SubState get() = getStateFct()

    override suspend fun SubState.update(block: SubState.() -> SubState) {
        updateStateFct(block)
    }

    override suspend fun transitionTo(block: SubState.() -> State): Nothing {
        transitionToFct(block)
        throw CancellationException()
    }

    override fun launch(block: LaunchBlockReceiver<State, SubState>): Job =
        stateScope.launch { block(this@LaunchBlockRuntime) }

    override fun launchInMachine(block: LaunchBlockReceiver<State, SubState>): Job =
        machineScope.launch { block(this@LaunchBlockRuntime) }
}
