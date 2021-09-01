package de.halfbit.comachine.tests

import de.halfbit.comachine.mutableComachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals

class DelegateOnAnyCanBeRegisteredTest {

    data class State(
        val position: Int = 0,
        val playing: Boolean = false,
        val saved: Boolean = false,
    )

    sealed interface Event {
        object Play : Event
        object Seek : Event
    }

    @Test
    fun test() {

        val machine = mutableComachine<State, Event>(startWith = State())

        machine.registerDelegate {
            whenIn<State> {
                onEnter {
                    launch {
                        state.update {
                            copy(saved = true)
                        }
                    }
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                on<Event.Play> {
                    state = state.copy(playing = true)
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                on<Event.Seek> {
                    state = state.copy(position = state.position + 1)
                }
            }
        }

        executeBlockingTest {
            val states = mutableListOf<State>()
            launch {
                machine.state.collect {
                    states += it
                    println("$it")
                }
            }

            machine.startInScope(this)
            machine.await<State> { saved }

            machine.send(Event.Seek)
            machine.await<State> { position == 1 }

            machine.send(Event.Play)
            machine.await<State> { playing }

            assertEquals(
                expected = listOf(
                    State(position = 0, playing = false, saved = false),
                    State(position = 0, playing = false, saved = true),
                    State(position = 1, playing = false, saved = true),
                    State(position = 1, playing = true, saved = true),
                ),
                actual = states
            )
        }
    }
}