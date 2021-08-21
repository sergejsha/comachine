package de.halfbit.comachine

import de.halfbit.comachine.dsl.ComachineBuilder
import kotlinx.coroutines.flow.Flow

interface Comachine<State : Any, Event : Any> {
    val state: Flow<State>
    suspend fun send(event: Event)
    suspend fun loop()
}

fun <State : Any, Event : Any> comachine(
    startWith: State,
    block: ComachineBuilder<State, Event>.() -> Unit
): Comachine<State, Event> =
    ComachineBuilder<State, Event>(startWith).also(block).build()
