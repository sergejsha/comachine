package de.halfbit.comachine.runtime

import de.halfbit.comachine.dsl.LaunchBlockReceiver
import de.halfbit.comachine.dsl.OnEnterBlock
import kotlinx.coroutines.Job
import kotlin.coroutines.cancellation.CancellationException

internal class OnEnterRuntime<State : Any, SubState : State>(
    private val getStateFct: () -> SubState,
    private val setStateFct: (SubState) -> Unit,
    private val transitionToFct: (State) -> Unit,
    private val launchInStateFct: (LaunchBlockReceiver<State, SubState>) -> Job,
    private val launchInMachineFct: (LaunchBlockReceiver<State, SubState>) -> Job,
) : OnEnterBlock<State, SubState> {

    override var state: SubState
        get() = getStateFct()
        set(value) {
            setStateFct(value)
        }

    override fun transitionTo(state: State): Nothing {
        transitionToFct(state)
        throw CancellationException()
    }

    override fun launch(block: LaunchBlockReceiver<State, SubState>) =
        launchInStateFct(block)

    override fun launchInMachine(block: LaunchBlockReceiver<State, SubState>) =
        launchInMachineFct(block)
}
