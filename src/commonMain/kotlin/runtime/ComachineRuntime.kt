package de.halfbit.comachine.runtime

import de.halfbit.comachine.dsl.WhenIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

internal typealias LaunchInState = (suspend () -> Unit) -> Job
internal typealias LaunchInMachine = (suspend () -> Unit) -> Unit
internal typealias EmitMessage = suspend (Message) -> Unit

internal sealed interface Message {
    data class OnStartedWith(val state: Any) : Message
    data class OnEventReceived(val event: Any) : Message
    data class OnEventCompleted(val event: Any) : Message
    data class OnCallback(val callback: suspend () -> Unit) : Message
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
                        onStartedWith(it.state as State)
                        onStarted?.complete(Unit)
                    }
                    is Message.OnEventReceived -> onEventReceived(it.event as Event)
                    is Message.OnEventCompleted -> onEventCompleted(it.event as Event)
                    is Message.OnCallback -> it.callback()
                }
            }
    }

    private fun launchInMachine(block: suspend () -> Unit) =
        machineScope.launch { block() }

    private fun <SubState : State> createWhereIn(
        state: SubState
    ): WhenInRuntime<State, SubState, Event>? =
        (whenIns[state::class] as? WhenIn<State, SubState>)
            ?.let {
                WhenInRuntime(
                    state = state,
                    whenIn = it,
                    machineScope = machineScope,
                    launchInMachineFct = ::launchInMachine,
                    onTransitionTo = ::onTransitionTo,
                    onUpdateState = ::onUpdateState,
                    emitMessage = messageFlow::emit,
                )
            }

    private suspend fun <SubState : State> gotoState(state: SubState) {
        stateFlow.emit(state)
        whereInRuntime = createWhereIn(state)
        whereInRuntime?.onEnter()
    }

    private suspend fun <SubState : State> onStartedWith(state: SubState) {
        gotoState(state)
    }

    private fun onEventReceived(event: Event) {
        checkNotNull(whereInRuntime) { "WhenIn block is missing for $event in $state" }
            .onEventReceived(event)
    }

    private fun onEventCompleted(event: Event) {
        whereInRuntime?.onEventCompleted(event)
    }

    private suspend fun onTransitionTo(state: State) {
        whereInRuntime?.onExit()
        gotoState(state)
    }

    private suspend fun onUpdateState(state: State) {
        stateFlow.emit(state)
    }
}
