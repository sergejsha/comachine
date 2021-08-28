package de.halfbit.comachine.dsl

import kotlinx.coroutines.Job
import kotlin.reflect.KClass

@PublishedApi
internal sealed interface OnEvent<State : Any, SubState : State, SubEvent : Any> {
    val eventType: KClass<SubEvent>

    class Default<State : Any, SubState : State, SubEvent : Any>(
        override val eventType: KClass<SubEvent>,
        val block: OnEventBlock<State, SubState>.(SubEvent) -> Unit,
    ) : OnEvent<State, SubState, SubEvent>

    class Launchable<State : Any, SubState : State, SubEvent : Any>(
        override val eventType: KClass<SubEvent>,
        val launchMode: LaunchMode,
        val block: suspend LaunchBlock<State, SubState>.(SubEvent) -> Unit,
    ) : OnEvent<State, SubState, SubEvent>
}

@PublishedApi
internal enum class LaunchMode {
    Sequential, Concurrent, Latest, Exclusive
}

@ComachineDsl
interface OnEventBlock<State : Any, SubState : State> {
    var state: SubState
    fun transitionTo(state: State): Nothing
    fun launch(block: LaunchBlockReceiver<State, SubState>): Job
    fun launchInMachine(block: LaunchBlockReceiver<State, SubState>): Job
}
