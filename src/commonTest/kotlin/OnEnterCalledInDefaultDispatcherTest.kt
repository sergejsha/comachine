package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.Test

class OnEnterCalledInDefaultDispatcherTest {

    sealed interface State {
        data class Zero(val count: Int) : State
        data class One(val count: Int) : State
        data class Two(val count: Int) : State
    }

    sealed interface Event {
        object One : Event
        object Two : Event
        object Three : Event
    }

    @Test
    fun test() {

        val machine = comachine<State, Event>(
            startWith = State.Zero(count = 0)
        ) {
            whenIn<State.Zero> {
                onEnter { state.update { copy(count = count + 1) } }
                on<Event.One> {
                    transitionTo(State.One(count = state.count))
                }
            }
            whenIn<State.One> {
                onEnter { state.update { copy(count = count + 1) } }
                on<Event.Two> {
                    transitionTo(State.Two(count = state.count))
                }
            }
            whenIn<State.Two> {
                onEnter { state.update { copy(count = count + 1) } }
                on<Event.Three> {
                    transitionTo(State.Zero(count = state.count))
                }
            }
        }

        executeBlockingTest {
            launch {
                machine.state.collect {
                    println("$it")
                }
            }

            machine.startInScope(this)

            machine.send(Event.One)
            machine.send(Event.Two)
            machine.send(Event.Three)

            machine.await<State.Zero> { count == 4 }
        }
    }
}