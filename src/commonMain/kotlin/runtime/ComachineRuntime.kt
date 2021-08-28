package de.halfbit.comachine.runtime

import de.halfbit.comachine.dsl.WhenIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onSubscription
import kotlin.reflect.KClass

internal typealias LaunchInState = (suspend () -> Unit) -> Job
internal typealias LaunchInMachine = (suspend () -> Unit) -> Job
internal typealias EmitMessage = suspend (Message) -> Unit

internal sealed interface Message {
    data class OnStartedWith(val state: Any) : Message
    data class OnEventReceived(val event: Any) : Message
    data class OnEventCompleted(val event: Any) : Message
    data class OnCallback(val callback: suspend () -> Unit) : Message
    data class OnEnterState(val state: Any) : Message
}

@Suppress("UNCHECKED_CAST")
internal class ComachineRuntime<State : Any, Event : Any>(
    private var state: State,
    private val machineScope: CoroutineScope,
    private val stateFlow: MutableSharedFlow<State>,
    private val whenIns: MutableMap<KClass<out State>, WhenIn<State, out State>>,
) {
    private val messageFlow = MutableSharedFlow<Message>()
    private var whereInRuntime: WhenInRuntime<State, out State, Event>? = null

    suspend fun send(event: Event) {
        messageFlow.emit(Message.OnEventReceived(event))
    }

    suspend fun loop(onStarted: CompletableDeferred<Unit>?) {
        messageFlow
            .onSubscription { emit(Message.OnStartedWith(state)) }
            .collect {
                when (it) {
                    is Message.OnStartedWith -> {
                        onEnterState(it.state as State)
                        onStarted?.complete(Unit)
                    }
                    is Message.OnEventReceived -> onEventReceived(it.event as Event)
                    is Message.OnEventCompleted -> onEventCompleted(it.event as Event)
                    is Message.OnCallback -> it.callback()
                    is Message.OnEnterState -> onEnterState(it.state as State)
                }
            }
    }

    private fun <SubState : State> createWhereIn(
        state: SubState
    ): WhenInRuntime<State, SubState, Event>? =
        (whenIns[state::class] as? WhenIn<State, SubState>)
            ?.let {
                WhenInRuntime(
                    state = state,
                    whenIn = it,
                    machineScope = machineScope,
                    transitionToFct = ::transitionTo,
                    emitStateFct = ::emitState,
                    emitMessage = messageFlow::emit,
                )
            }

    private fun onEventReceived(event: Event) {
        checkNotNull(whereInRuntime) { "WhenIn block is missing for $event in $state" }
            .onEventReceived(event)
    }

    private fun onEventCompleted(event: Event) {
        whereInRuntime?.onEventCompleted(event)
    }

    private fun transitionTo(state: State) {
        whereInRuntime?.onExit()
        onEnterState(state)
    }

    private fun onEnterState(state: State) {
        emitState(state)
        whereInRuntime = createWhereIn(state)
        whereInRuntime?.onEnter()
    }

    private fun emitState(state: State) {
        check(stateFlow.tryEmit(state)) {
            reportError("StateFlow suspends although it never should.")
        }
    }
}

internal fun reportError(message: String): String =
    "$message Please report this bug to https://github.com/beworker/comachine/"
