package de.halfbit.comachine

import de.halfbit.comachine.dsl.ComachineBlock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow

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
