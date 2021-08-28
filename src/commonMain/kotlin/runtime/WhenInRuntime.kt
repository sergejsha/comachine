package de.halfbit.comachine.runtime

import de.halfbit.comachine.dsl.LaunchBlock
import de.halfbit.comachine.dsl.LaunchBlockReceiver
import de.halfbit.comachine.dsl.LaunchMode
import de.halfbit.comachine.dsl.OnEvent
import de.halfbit.comachine.dsl.OnEventBlock
import de.halfbit.comachine.dsl.WhenIn
import de.halfbit.comachine.runtime.dispatchers.ConcurrentEventDispatcher
import de.halfbit.comachine.runtime.dispatchers.DefaultEventDispatcher
import de.halfbit.comachine.runtime.dispatchers.LatestEventDispatcher
import de.halfbit.comachine.runtime.dispatchers.SequentialEventDispatcher
import de.halfbit.comachine.runtime.dispatchers.SingleEventDispatcher
import kotlinx.coroutines.CancellationException
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
    private val eventDispatchers: MutableMap<KClass<out Event>, EventDispatcher<Event>> =
        mutableMapOf()

    private val stateScope: CoroutineScope by lazy {
        CoroutineScope(
            SupervisorJob(machineScope.coroutineContext[Job])
        )
    }

    private val launchBlock: LaunchBlock<State, SubState> by lazy {
        LaunchBlockRuntime(
            getStateFct = ::getState,
            stateScope = stateScope,
            machineScope = machineScope,
            updateStateFct = ::updateState,
            transitionToFct = ::transitionTo,
        )
    }

    private val eventRuntime: OnEventBlock<State, SubState> by lazy {
        OnEventRuntime(
            getStateFct = ::getState,
            setStateFct = ::setState,
            launchInStateFct = ::launchInState,
            launchInMachineFct = ::launchInMachine,
            transitionToFct = transitionToFct,
        )
    }

    private fun launchInState(block: suspend () -> Unit) =
        stateScope.launch { block() }

    private fun launchInState(block: LaunchBlockReceiver<State, SubState>) =
        stateScope.launch { block(launchBlock) }

    private fun launchInMachine(block: suspend () -> Unit) =
        machineScope.launch { block() }

    private fun launchInMachine(block: LaunchBlockReceiver<State, SubState>) =
        machineScope.launch { block(launchBlock) }

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
    private fun createEventDispatcher(onEvent: OnEvent<State, SubState, *>) =
        when (onEvent) {
            is OnEvent.Default -> DefaultEventDispatcher(
                eventRuntime = eventRuntime,
                block = onEvent.block,
            )
            is OnEvent.Launchable -> when (onEvent.launchMode) {
                LaunchMode.Sequential ->
                    SequentialEventDispatcher(
                        block = onEvent.block,
                        launchInStateFct = ::launchInState,
                        emitMessage = emitMessage,
                        launchBlock = launchBlock
                    )
                LaunchMode.Concurrent ->
                    ConcurrentEventDispatcher(
                        block = onEvent.block,
                        launchInStateFct = ::launchInState,
                        emitMessage = emitMessage,
                        launchBlock = launchBlock
                    )
                LaunchMode.Single ->
                    SingleEventDispatcher(
                        block = onEvent.block,
                        launchInStateFct = ::launchInState,
                        emitMessage = emitMessage,
                        launchBlock = launchBlock
                    )
                LaunchMode.Latest ->
                    LatestEventDispatcher(
                        block = onEvent.block,
                        launchInStateFct = ::launchInState,
                        emitMessage = emitMessage,
                        launchBlock = launchBlock
                    )
            }
        } as EventDispatcher<Event>

    private fun getEventRuntimeOrNull(eventType: KClass<out Event>) =
        eventDispatchers[eventType]
            ?: whenIn.onEvent[eventType]?.let { onEvent ->
                createEventDispatcher(onEvent)
                    .also { eventDispatchers[eventType] = it }
            }

    fun onEnter() {
        whenIn.onEnter?.let { onEnter ->
            onEnter.block(eventRuntime)
        }
    }

    fun onEventReceived(event: Event) {
        getEventRuntimeOrNull(event::class)?.let { eventRuntime ->
            try {
                eventRuntime.onEventReceived(event)
            } catch (err: CancellationException) {
                stateScope.cancel(err)
            }
        }
    }

    fun onEventCompleted(event: Event) {
        eventDispatchers[event::class]?.let { eventRuntime ->
            try {
                eventRuntime.onEventCompleted(event)
            } catch (err: CancellationException) {
                stateScope.cancel(err)
            }
        }
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

internal interface EventDispatcher<SubEvent : Any> {
    fun onEventReceived(event: SubEvent)
    fun onEventCompleted(event: SubEvent) {}
}

private class LaunchBlockRuntime<State : Any, SubState : State>(
    private val getStateFct: () -> SubState,
    private val stateScope: CoroutineScope,
    private val machineScope: CoroutineScope,
    private val updateStateFct: suspend ((SubState) -> SubState) -> Unit,
    private val transitionToFct: suspend ((SubState) -> State) -> Unit,
) : LaunchBlock<State, SubState> {

    override val state: SubState get() = getStateFct()

    override suspend fun SubState.update(block: SubState.() -> SubState) {
        updateStateFct(block)
    }

    override suspend fun transitionTo(block: SubState.() -> State): Nothing {
        transitionToFct(block)
        throw TransitionPerformedException()
    }

    override fun launch(block: LaunchBlockReceiver<State, SubState>): Job =
        stateScope.launch { block(this@LaunchBlockRuntime) }

    override fun launchInMachine(block: LaunchBlockReceiver<State, SubState>): Job =
        machineScope.launch { block(this@LaunchBlockRuntime) }
}

internal class TransitionPerformedException : CancellationException("transition performed")