package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.Test

class AggregateStatesTest {

    sealed interface State {
        object Solid : State
        object Liquid : State
        object Gas : State
    }


    sealed interface Event {
        object OnHeat : Event
        object OnCold : Event
    }

    @Test
    fun test_Comachine() {

        val comachine = comachine<State, Event>(
            startWith = State.Solid
        ) {
            whenIn<State.Solid> {
                onExclusive<Event.OnHeat> {
                    println("melting...")
                    delay(500)
                    println("melted")
                    transitionTo { State.Liquid }
                }
            }

            whenIn<State.Liquid> {
                onExclusive<Event.OnHeat> {
                    println("vaporizing...")
                    delay(500)
                    println("vaporized")
                    transitionTo { State.Gas }
                }
                onExclusive<Event.OnCold> {
                    println("freezing...")
                    delay(500)
                    println("frozen")
                    transitionTo { State.Solid }
                }
            }

            whenIn<State.Gas> {
                onExclusive<Event.OnCold> {
                    println("condensing...")
                    delay(500)
                    println("condensed")
                    transitionTo { State.Liquid }
                }
            }
        }

        runBlockingTest {
            launch {
                comachine.state.collect {
                    println("    ------ ${it::class.simpleName} ------")
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