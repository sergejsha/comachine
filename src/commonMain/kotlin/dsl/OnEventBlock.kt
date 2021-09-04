package de.halfbit.comachine.dsl

import de.halfbit.comachine.runtime.StateHolder
import de.halfbit.comachine.runtime.TransitionPerformedException
import kotlinx.coroutines.Job
import kotlin.reflect.KClass

@ComachineDsl
class OnEventBlock<State : Any, SubState : State>
internal constructor(
    @PublishedApi internal val stateHolder: StateHolder<State, SubState>,
    @PublishedApi internal val extras: Lazy<MutableMap<KClass<*>, Any?>>,
    @PublishedApi internal val transitionToFct: (State) -> Unit,
    private val launchInStateFct: (LaunchBlockReceiver<State, SubState>) -> Job,
    private val launchInMachineFct: (LaunchBlockReceiver<State, SubState>) -> Job,
) {

    val state: SubState
        get() = stateHolder.get()

    inline fun SubState.update(block: SubState.() -> SubState) {
        stateHolder.set(block(stateHolder.get()))
    }

    inline fun <reified T> getExtra(): T {
        return extras.value[T::class] as T
    }

    inline fun <reified T> setExtra(value: T) {
        extras.value[T::class] = value
    }

    inline fun <reified T> removeExtra() {
        extras.value.remove(T::class)
    }

    inline fun <reified T> hasExtra(): Boolean {
        return extras.value.containsKey(T::class)
    }

    inline fun transitionTo(block: SubState.() -> State): Nothing {
        transitionToFct(block(state))
        throw TransitionPerformedException()
    }

    fun launchInState(block: LaunchBlockReceiver<State, SubState>) =
        launchInStateFct(block)

    fun launchInMachine(block: LaunchBlockReceiver<State, SubState>) =
        launchInMachineFct(block)
}
