package de.halfbit.comachine.tests

import de.halfbit.comachine.mutableComachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class DelegateOnEventTest {

    data class State(
        val position: Int = 0,
        val playing: Boolean = false,
        val saved: Boolean = false,
    )

    object Event

    @Test
    fun multipleSecondaryHandlersCanBeRegistered() {

        val machine = mutableComachine<State, Event>(startWith = State())

        machine.registerDelegate {
            whenIn<State> {
                on<Event> {
                    state = state.copy(position = 1)
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                on<Event> {
                    state = state.copy(playing = true)
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                on<Event> {
                    state = state.copy(saved = true)
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
            machine.send(Event)
            machine.await<State> { position == 1 && saved && playing }

            assertEquals(
                expected = listOf(
                    State(position = 0, playing = false, saved = false),
                    State(position = 1, playing = false, saved = false),
                    State(position = 1, playing = true, saved = false),
                    State(position = 1, playing = true, saved = true),
                ),
                actual = states
            )
        }
    }

    @Test
    fun singleFirstMainHandlerCanBeRegistered() {

        val machine = mutableComachine<State, Event>(startWith = State())

        machine.registerDelegate {
            whenIn<State> {
                on<Event>(main = true) {
                    state = state.copy(position = 1)
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                on<Event> {
                    state = state.copy(playing = true)
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                on<Event> {
                    state = state.copy(saved = true)
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
            machine.send(Event)
            machine.await<State> { position == 1 && saved && playing }

            assertEquals(
                expected = listOf(
                    State(position = 0, playing = false, saved = false),
                    State(position = 1, playing = false, saved = false),
                    State(position = 1, playing = true, saved = false),
                    State(position = 1, playing = true, saved = true),
                ),
                actual = states
            )
        }
    }

    @Test
    fun singleSecondMainHandlerCanBeRegistered() {

        val machine = mutableComachine<State, Event>(startWith = State())

        machine.registerDelegate {
            whenIn<State> {
                on<Event> {
                    state = state.copy(position = 1)
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                on<Event>(main = true) {
                    state = state.copy(playing = true)
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                on<Event> {
                    state = state.copy(saved = true)
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
            machine.send(Event)
            machine.await<State> { position == 1 && saved && playing }

            assertEquals(
                expected = listOf(
                    State(position = 0, playing = false, saved = false),
                    State(position = 0, playing = true, saved = false),
                    State(position = 1, playing = true, saved = false),
                    State(position = 1, playing = true, saved = true),
                ),
                actual = states
            )
        }
    }

    @Test
    fun multipleMainHandlersCannotBeRegistered() {

        val machine = mutableComachine<State, Event>(startWith = State())

        machine.registerDelegate {
            whenIn<State> {
                on<Event> {
                    state = state.copy(position = 1)
                }
            }
        }

        machine.registerDelegate {
            whenIn<State> {
                on<Event>(main = true) {
                    state = state.copy(playing = true)
                }
            }
        }

        try {
            machine.registerDelegate {
                whenIn<State> {
                    on<Event>(main = true) {
                        state = state.copy(saved = true)
                    }
                }
            }
        } catch (err: IllegalArgumentException) {
            assertTrue(err.message?.contains("on<Event>") == true)
            return
        }

        fail("IllegalArgumentException not thrown")

    }
}