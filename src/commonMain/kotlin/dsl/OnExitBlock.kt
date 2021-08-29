package de.halfbit.comachine.dsl

import kotlinx.coroutines.Job

private typealias LaunchInMachine = (suspend () -> Unit) -> Job

@ComachineDsl
class OnExitBlock<SubState : Any>
@PublishedApi
internal constructor(
    private val getStateFct: () -> SubState,
    private val launchInMachineFct: LaunchInMachine,
) {

    val state: SubState
        get() = getStateFct()

    fun launchInMachine(block: suspend () -> Unit) {
        launchInMachineFct(block)
    }
}
