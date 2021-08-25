package de.halfbit.comachine.runtime

import de.halfbit.comachine.dsl.OnEventBlock
import kotlin.coroutines.cancellation.CancellationException

internal class OnEventRuntime<State : Any, SubState : State>(
    private val getStateFct: () -> SubState,
    private val launchInStateFct: LaunchInState,
    private val launchInMachineFct: LaunchInMachine,
    private val updateStateFct: suspend ((SubState) -> SubState) -> Unit,
    private val transitionToFct: suspend ((SubState) -> State) -> Unit,
) : OnEventBlock<State, SubState> {

    override val state: SubState
        get() = getStateFct()

    override suspend fun SubState.update(block: SubState.() -> SubState) {
        updateStateFct(block)
    }

    override suspend fun transitionTo(block: SubState.() -> State): Nothing {
        transitionToFct(block)
        throw CancellationException()
    }

    override fun launch(block: suspend () -> Unit) =
        launchInStateFct(block)


    override fun launchInMachine(block: suspend () -> Unit) =
        launchInMachineFct(block)
}
