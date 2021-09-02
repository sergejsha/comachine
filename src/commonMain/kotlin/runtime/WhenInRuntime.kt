package de.halfbit.comachine.runtime

import de.halfbit.comachine.dsl.LaunchBlock
import de.halfbit.comachine.dsl.LaunchBlockReceiver
import de.halfbit.comachine.dsl.LaunchMode
import de.halfbit.comachine.dsl.OnEvent
import de.halfbit.comachine.dsl.OnEventBlock
import de.halfbit.comachine.dsl.OnExitBlock
import de.halfbit.comachine.dsl.WhenIn
import de.halfbit.comachine.runtime.dispatchers.ConcurrentEventDispatcher
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
    private val stateHolder: StateHolder<State, SubState>,
    private val whenIn: WhenIn<State, SubState>,
    private val machineScope: CoroutineScope,
    private val transitionToFct: (State) -> Unit,
    private val emitMessage: EmitMessage,
) {

    private val eventDispatchers =
        mutableMapOf<KClass<out Event>, EventDispatcher<Event>>()

    private val extras =
        lazy { mutableMapOf<KClass<*>, Any?>() }

    private val stateScope: CoroutineScope by lazy {
        CoroutineScope(
            SupervisorJob(machineScope.coroutineContext[Job])
        )
    }

    private val launchBlock: LaunchBlock<State, SubState> by lazy {
        LaunchBlock(
            stateHolder = stateHolder,
            stateScope = stateScope,
            machineScope = machineScope,
            updateStateFct = ::updateState,
            transitionToFct = ::transitionTo,
        )
    }

    private val eventRuntime: OnEventBlock<State, SubState> by lazy {
        OnEventBlock(
            stateHolder = stateHolder,
            extras = extras,
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

    private suspend fun updateState(block: (SubState) -> SubState) {
        val called = CompletableDeferred<Unit>()
        stateScope.launch {
            emitMessage(
                Message.OnCallback(
                    callback = {
                        if (stateScope.isActive) {
                            stateHolder.set(block(stateHolder.get()))
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
                            transitionToFct(block(stateHolder.get()))
                        }
                        called.complete(Unit)
                    }
                )
            )
        }
        called.await()
    }

    fun onEnter() {
        var onEnter = whenIn.onEnter
        if (onEnter != null) {
            try {
                while (onEnter != null) {
                    onEnter.block.invoke(eventRuntime)
                    onEnter = onEnter.next
                }
            } catch (err: CancellationException) {
                stateScope.cancel(err)
            }
        }
    }

    fun onEventReceived(event: Event) {
        val eventType = event::class
        val onEvent = whenIn.onEvent[eventType] ?: return
        when (onEvent) {
            is OnEvent.NonSuspendable -> {
                var nextOnEvent = onEvent as OnEvent.NonSuspendable<State, SubState, Event>?
                try {
                    while (nextOnEvent != null) {
                        nextOnEvent.block(eventRuntime, event)
                        nextOnEvent = nextOnEvent.next
                    }
                } catch (err: CancellationException) {
                    stateScope.cancel(err)
                }
            }
            is OnEvent.Suspendable -> {
                val eventDispatcher = eventDispatchers[eventType]
                    ?: createEventDispatcher(onEvent)
                        .also { eventDispatchers[eventType] = it }
                try {
                    eventDispatcher.onEventReceived(event)
                } catch (err: CancellationException) {
                    stateScope.cancel(err)
                }
            }
        }
    }

    private fun createEventDispatcher(onEvent: OnEvent.Suspendable<State, SubState, *>) =
        when (onEvent.launchMode) {
            LaunchMode.Sequential ->
                SequentialEventDispatcher(
                    block = onEvent.block,
                    launchBlock = launchBlock,
                    launchInStateFct = ::launchInState,
                    emitMessage = emitMessage,
                )
            LaunchMode.Concurrent ->
                ConcurrentEventDispatcher(
                    block = onEvent.block,
                    launchBlock = launchBlock,
                    launchInStateFct = ::launchInState,
                    emitMessage = emitMessage,
                )
            LaunchMode.Single ->
                SingleEventDispatcher(
                    block = onEvent.block,
                    launchBlock = launchBlock,
                    launchInStateFct = ::launchInState,
                    emitMessage = emitMessage,
                )
            LaunchMode.Latest ->
                LatestEventDispatcher(
                    block = onEvent.block,
                    launchBlock = launchBlock,
                    launchInStateFct = ::launchInState,
                    emitMessage = emitMessage,
                )
        } as EventDispatcher<Event>

    fun onEventCompleted(event: Event) {
        eventDispatchers[event::class]?.let { eventDispatcher ->
            try {
                eventDispatcher.onEventCompleted(event)
            } catch (err: CancellationException) {
                stateScope.cancel(err)
            }
        }
    }

    fun onExit() {
        var onExit = whenIn.onExit
        if (onExit != null) {
            val onExitBlock = OnExitBlock(
                getStateFct = stateHolder::get,
                extras = extras,
                launchInMachineFct = ::launchInMachine,
            )
            try {
                while (onExit != null) {
                    onExit.block.invoke(onExitBlock)
                    onExit = onExit.next
                }
            } catch (err: CancellationException) {
                stateScope.cancel(err)
            }
        }
        if (stateScope.isActive) {
            stateScope.cancel()
        }
    }
}

internal typealias LaunchInState = (suspend () -> Unit) -> Job

internal interface EventDispatcher<SubEvent : Any> {
    fun onEventReceived(event: SubEvent)
    fun onEventCompleted(event: SubEvent) {}
}

internal class TransitionPerformedException : CancellationException("cancelled on transition")
