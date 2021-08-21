package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
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
                    delay(500)
                    state.update { copy(progress = "melted") }
                    transitionTo { State.Liquid() }
                }
            }

            whenIn<State.Liquid> {
                onExclusive<Event.OnHeat> {
                    state.update { copy(progress = "vaporizing...") }
                    delay(500)
                    state.update { copy(progress = "vaporized") }
                    transitionTo { State.Gas() }
                }
                onExclusive<Event.OnCold> {
                    state.update { copy(progress = "freezing...") }
                    delay(500)
                    state.update { copy(progress = "frozen") }
                    transitionTo { State.Solid() }
                }
            }

            whenIn<State.Gas> {
                onExclusive<Event.OnCold> {
                    state.update { copy(progress = "condensing...") }
                    delay(500)
                    state.update { copy(progress = "frozen") }
                    transitionTo { State.Liquid() }
                }
            }
        }

        runBlockingTest {
            launch {
                comachine.state.collect {
                    println("----- $it")
                }
            }
            launch { comachine.loop() }
            delay(500)

            println("OnHeat")
            comachine.send(Event.OnHeat)
            delay(1000)

            println("OnHeat")
            comachine.send(Event.OnHeat)
            delay(1000)

            println("OnCold")
            comachine.send(Event.OnCold)
            delay(1000)

            println("OnCold")
            comachine.send(Event.OnCold)
            delay(1000)

            coroutineContext.cancelChildren()
        }
    }
}