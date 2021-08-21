package de.halfbit.comachine.runtime

import de.halfbit.comachine.dsl.EventDispatching
import de.halfbit.comachine.dsl.WhenIn
import de.halfbit.comachine.runtime.dispatchers.SequentialEventDispatcher
import de.halfbit.comachine.runtime.dispatchers.ConcurrentEventDispatcher
import de.halfbit.comachine.runtime.dispatchers.ExclusiveEventDispatcher
import de.halfbit.comachine.runtime.dispatchers.LatestEventDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass

private typealias EventRuntimeMap<Event> =
    MutableMap<KClass<out Event>, OnEventDispatcher<Event>>

internal class WhenInRuntime<State : Any, SubState : State, Event : Any>(
    private var state: SubState,
    private val whenIn: WhenIn<State, SubState>,
    private val machineScope: CoroutineScope,
    private val launchInMachineFct: LaunchInMachine,
    private val onTransitionTo: suspend (State) -> Unit,
    private val onUpdateState: suspend (State) -> Unit,
    private val emitMessage: EmitMessage,
) {
    private val eventRuntimes: EventRuntimeMap<Event> = mutableMapOf()

    private val stateScope: CoroutineScope by lazy {
        CoroutineScope(
            SupervisorJob(machineScope.coroutineContext.job)
        )
    }

    private fun launchInState(block: suspend () -> Unit) =
        stateScope.launch { block() }

    private fun getSubState(): SubState {
        return state
    }

    private suspend fun updateState(block: (SubState) -> SubState) {
        suspendCoroutine<Unit> { cont ->
            stateScope.launch {
                emitMessage(
                    Message.OnCallback(
                        callback = {
                            if (stateScope.isActive) {
                                state = block(state)
                                onUpdateState(state)
                            }
                            cont.resume(Unit)
                        }
                    )
                )
            }
        }
    }

    private suspend fun transitionTo(block: (SubState) -> State) {
        suspendCoroutine<Unit> { cont ->
            stateScope.launch {
                emitMessage(
                    Message.OnCallback(
                        callback = {
                            if (stateScope.isActive) {
                                onTransitionTo(block(state))
                            }
                            cont.resume(Unit)
                        }
                    )
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createRuntimeOrNull(eventType: KClass<out Event>): OnEventDispatcher<Event>? =
        whenIn.onEvent
            .find { it.eventType == eventType }
            ?.let { onEvent ->
                val onEventRuntime = OnEventRuntime<State, SubState>(
                    getStateFct = ::getSubState,
                    launchInStateFct = ::launchInState,
                    launchInMachineFct = launchInMachineFct,
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
            val onEnterRuntime = OnEnterRuntime<State, SubState>(
                getStateFct = ::getSubState,
                launchInStateFct = ::launchInState,
                launchInMachineFct = launchInMachineFct,
                updateStateFct = ::updateState,
                transitionToFct = ::transitionTo,
            )
            launchInState { onEnter.block(onEnterRuntime) }
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
                getStateFct = ::getSubState,
                launchInMachineFct = launchInMachineFct,
            )
            onExit.block(onExitRuntime)
        }
        stateScope.cancel()
    }
}
