package de.halfbit.comachine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
