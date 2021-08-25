package de.halfbit.comachine.runtime

import de.halfbit.comachine.dsl.EventDispatching
import de.halfbit.comachine.dsl.LaunchBlock
import de.halfbit.comachine.dsl.LaunchBlockReceiver
import de.halfbit.comachine.dsl.WhenIn
import de.halfbit.comachine.runtime.dispatchers.ConcurrentEventDispatcher
import de.halfbit.comachine.runtime.dispatchers.ExclusiveEventDispatcher
import de.halfbit.comachine.runtime.dispatchers.LatestEventDispatcher
import de.halfbit.comachine.runtime.dispatchers.SequentialEventDispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

internal class WhenInRuntime<State : Any, SubState : State, Event : Any>(
    private var state: SubState,
    private val whenIn: WhenIn<State, SubState>,
    private val machineScope: CoroutineScope,
    private val transitionToFct: (State) -> Unit,
    private val emitStateFct: (State) -> Unit,
    private val emitMessage: EmitMessage,
) {
    private val eventRuntimes: MutableMap<KClass<out Event>, OnEventDispatcher<Event>> =
        mutableMapOf()

    private val stateScope: CoroutineScope by lazy {
        CoroutineScope(
            SupervisorJob(machineScope.coroutineContext[Job])
        )
    }

    private val launchBlockReceiver: LaunchBlock<State, SubState> by lazy {
        LaunchBlockRuntime(
            getStateFct = ::getState,
            stateScope = stateScope,
            machineScope = machineScope,
            updateStateFct = ::updateState,
            transitionToFct = ::transitionTo,
        )
    }

    private fun launchInState(block: suspend () -> Unit) =
        stateScope.launch { block() }

    private fun launchInState(block: LaunchBlockReceiver<State, SubState>) =
        stateScope.launch { block(launchBlockReceiver) }

    private fun launchInMachine(block: suspend () -> Unit) =
        machineScope.launch { block() }

    private fun launchInMachine(block: LaunchBlockReceiver<State, SubState>) =
        machineScope.launch { block(launchBlockReceiver) }

    private fun getState(): SubState = state
    private fun setState(newState: SubState) {
        state = newState
    }

    private fun updateState(newState: SubState) {
        state = newState
        emitStateFct(newState)
    }

    private suspend fun updateState(block: (SubState) -> SubState) {
        val called = CompletableDeferred<Unit>()
        stateScope.launch {
            emitMessage(
                Message.OnCallback(
                    callback = {
                        if (stateScope.isActive) {
                            updateState(block(state))
                        }
                        called.complete(Unit)
                    }
                )
            )
        }
        called.await()
    }

    private suspend fun transitionTo(block: (SubState) -> State) {
        val called = CompletableDeferred<Unit>()
        stateScope.launch {
            emitMessage(
                Message.OnCallback(
                    callback = {
                        if (stateScope.isActive) {
                            transitionToFct(block(state))
                        }
                        called.complete(Unit)
                    }
                )
            )
        }
        called.await()
    }

    @Suppress("UNCHECKED_CAST")
    private fun createRuntimeOrNull(eventType: KClass<out Event>): OnEventDispatcher<Event>? =
        whenIn.onEvent[eventType]?.let { onEvent ->
            val onEventRuntime = OnEventRuntime<State, SubState>(
                getStateFct = ::getState,
                launchInStateFct = ::launchInState,
                launchInMachineFct = ::launchInMachine,
                updateStateFct = ::updateState,
                transitionToFct = ::transitionTo,
            )
            when (onEvent.eventDispatching) {
                EventDispatching.Sequential ->
                    SequentialEventDispatcher(
                        onEvent = onEvent,
                        launchInStateFct = ::launchInState,
                        emitMessage = emitMessage,
                        onEventRuntime = onEventRuntime
                    )
                EventDispatching.Concurrent ->
                    ConcurrentEventDispatcher(
                        onEvent = onEvent,
                        launchInStateFct = ::launchInState,
                        emitMessage = emitMessage,
                        onEventRuntime = onEventRuntime
                    )
                EventDispatching.Exclusive ->
                    ExclusiveEventDispatcher(
                        onEvent = onEvent,
                        launchInStateFct = ::launchInState,
                        emitMessage = emitMessage,
                        onEventRuntime = onEventRuntime
                    )
                EventDispatching.Latest ->
                    LatestEventDispatcher(
                        onEvent = onEvent,
                        launchInStateFct = ::launchInState,
                        emitMessage = emitMessage,
                        onEventRuntime = onEventRuntime
                    )
            } as OnEventDispatcher<Event>
        }

    fun onEnter() {
        whenIn.onEnter?.let { onEnter ->
            val onEnterRuntime = OnEnterRuntime(
                getStateFct = ::getState,
                setStateFct = ::setState,
                launchInStateFct = ::launchInState,
                launchInMachineFct = ::launchInMachine,
                transitionToFct = transitionToFct,
            )
            onEnter.block(onEnterRuntime)
        }
    }

    fun onEventReceived(event: Event) {
        val eventType = event::class
        val eventRuntime = eventRuntimes[eventType]
            ?: createRuntimeOrNull(eventType)
                ?.also { eventRuntimes[eventType] = it }
        eventRuntime?.onEventReceived(event)
    }

    fun onEventCompleted(event: Event) {
        eventRuntimes[event::class]?.onEventCompleted(event)
    }

    fun onExit() {
        whenIn.onExit?.let { onExit ->
            val onExitRuntime = OnExitRuntime(
                getStateFct = ::getState,
                launchInMachineFct = ::launchInMachine,
            )
            onExit.block(onExitRuntime)
        }
        stateScope.cancel()
    }
}
