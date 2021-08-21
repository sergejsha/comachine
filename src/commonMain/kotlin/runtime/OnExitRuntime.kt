package de.halfbit.comachine.runtime

import de.halfbit.comachine.dsl.OnExitBlock

internal class OnExitRuntime<SubState : Any>(
    private val getStateFct: () -> SubState,
    private val launchInMachineFct: LaunchInMachine,
) : OnExitBlock<SubState> {

    override val state: SubState
        get() = getStateFct()

    override fun launchInMachine(block: suspend () -> Unit) {
        launchInMachineFct(block)
    }
}
