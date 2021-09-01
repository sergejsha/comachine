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

class DelegateOnExitTest {

    sealed interface State {
        object One : State
        object Two : State
    }

    @Test
    fun multipleSecondaryHandlersCanBeRegistered() {

        val machine = mutableComachine<State, Unit>(startWith = State.One)
        var result = ""

        machine.registerDelegate {
            whenIn<State.One> {
                onExit {
                    result += "1"
                }
            }
        }

        machine.registerDelegate {
            whenIn<State.One> {
                onExit {
                    result += "2"
                }
            }
        }

        machine.registerDelegate {
            whenIn<State.One> {
                onEnter { transitionTo(State.Two) }
                onExit {
                    result += "3"
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
            machine.await<State.Two>()

            assertEquals(
                expected = "123",
                actual = result
            )
        }
    }

    @Test
    fun singleMainHandlerCanBeRegistered() {

        val machine = mutableComachine<State, Unit>(startWith = State.One)
        var result = ""

        machine.registerDelegate {
            whenIn<State.One> {
                onExit {
                    result += "1"
                }
            }
        }

        machine.registerDelegate {
            whenIn<State.One> {
                onExit(main = true) {
                    result += "2"
                }
            }
        }

        machine.registerDelegate {
            whenIn<State.One> {
                onEnter { transitionTo(State.Two) }
                onExit {
                    result += "3"
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
            machine.await<State.Two>()

            assertEquals(
                expected = "213",
                actual = result
            )
        }
    }

    @Test
    fun multipleMainHandlersCannotBeRegistered() {

        val machine = mutableComachine<State, Unit>(startWith = State.One)
        var result = ""

        machine.registerDelegate {
            whenIn<State.One> {
                onExit {
                    result += "1"
                }
            }
        }

        machine.registerDelegate {
            whenIn<State.One> {
                onExit(main = true) {
                    result += "2"
                }
            }
        }

        try {
            machine.registerDelegate {
                whenIn<State.One> {
                    onEnter { transitionTo(State.Two) }
                    onExit(main = true) {
                        result += "3"
                    }
                }
            }
        } catch (err: IllegalArgumentException) {
            assertTrue(err.message?.contains("onExit") == true)
            return
        }

        fail("IllegalArgumentException not thrown")
    }
}