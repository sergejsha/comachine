package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.Test

class AggregateStatesTest {

    sealed interface State {
        data class Solid(val progress: String = "") : State
        data class Liquid(val progress: String = "") : State
        data class Gas(val progress: String = "") : State
    }


    sealed interface Event {
        object OnHeat : Event
        object OnCold : Event
    }

    @Test
    fun test_Comachine() {

        val comachine = comachine<State, Event>(
            startWith = State.Solid()
        ) {
            whenIn<State.Solid> {
                onExclusive<Event.OnHeat> {
                    state.update { copy(progress = "melting...") }
                    delay(100)
                    state.update { copy(progress = "melted") }
                    transitionTo { State.Liquid() }
                }
            }

            whenIn<State.Liquid> {
                onExclusive<Event.OnHeat> {
                    state.update { copy(progress = "vaporizing...") }
                    delay(100)
                    state.update { copy(progress = "vaporized") }
                    transitionTo { State.Gas() }
                }
                onExclusive<Event.OnCold> {
                    state.update { copy(progress = "freezing...") }
                    delay(100)
                    state.update { copy(progress = "frozen") }
                    transitionTo { State.Solid() }
                }
            }

            whenIn<State.Gas> {
                onExclusive<Event.OnCold> {
                    state.update { copy(progress = "condensing...") }
                    delay(100)
                    state.update { copy(progress = "frozen") }
                    transitionTo { State.Liquid() }
                }
            }
        }

        executeBlocking {
            launch {
                comachine.state.collect {
                    println("------ $it")
                }
            }
            comachine.startInScope(this)

            println("OnHeat")
            comachine.send(Event.OnHeat)
            comachine.await<State.Liquid>()

            println("OnHeat")
            comachine.send(Event.OnHeat)
            comachine.await<State.Gas>()

            println("OnCold")
            comachine.send(Event.OnCold)
            comachine.await<State.Liquid>()

            println("OnCold")
            comachine.send(Event.OnCold)
            comachine.await<State.Solid>()

            coroutineContext.cancelChildren()
        }
    }
}