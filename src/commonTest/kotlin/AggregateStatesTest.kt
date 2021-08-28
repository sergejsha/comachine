package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlockingTest
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
                on<Event.OnHeat> {
                    transitionTo(State.Liquid())
                }
            }
            whenIn<State.Liquid> {
                on<Event.OnHeat> {
                    transitionTo(State.Gas())
                }
                on<Event.OnCold> {
                    transitionTo(State.Solid())
                }
            }
            whenIn<State.Gas> {
                on<Event.OnCold> {
                    transitionTo(State.Liquid())
                }
            }
        }

        executeBlockingTest {
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
        }
    }
}