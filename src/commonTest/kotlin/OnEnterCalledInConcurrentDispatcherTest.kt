package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.Test

class OnEnterCalledInConcurrentDispatcherTest {

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
                onConcurrent<Event.One> {
                    transitionTo { State.One(count = count) }
                }
            }
            whenIn<State.One> {
                onEnter { state.update { copy(count = count + 1) } }
                onConcurrent<Event.Two> {
                    transitionTo { State.Two(count = count) }
                }
            }
            whenIn<State.Two> {
                onEnter { state.update { copy(count = count + 1) } }
                onConcurrent<Event.Three> {
                    transitionTo { State.Zero(count = count) }
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
            machine.await<State.One>()

            machine.send(Event.Two)
            machine.await<State.Two>()

            machine.send(Event.Three)
            machine.await<State.Zero> { count == 4 }
        }
    }
}