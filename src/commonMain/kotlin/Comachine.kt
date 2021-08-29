package de.halfbit.comachine

import de.halfbit.comachine.dsl.ComachineBlock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

interface Comachine<State : Any, Event : Any> {
    val state: Flow<State>
    suspend fun send(event: Event)
    suspend fun loop(onStarted: CompletableDeferred<Unit>? = null)
}

fun <State : Any, Event : Any> comachine(
    startWith: State,
    block: ComachineBlock<State, Event>.() -> Unit
): Comachine<State, Event> =
    ComachineBlock<State, Event>(startWith).also(block).build()

/**
 * Launches the machine in the given scope and suspends until the initial state is
 * emitted. The machine is fully prepared and usable after this method exists.
 *
 * @return the coroutine job, in which the machine processes the events.
 */
suspend fun Comachine<*, *>.startInScope(scope: CoroutineScope): Job {
    val onStarted = CompletableDeferred<Unit>()
    val job = scope.launch { loop(onStarted) }
    onStarted.await()
    return job
}
