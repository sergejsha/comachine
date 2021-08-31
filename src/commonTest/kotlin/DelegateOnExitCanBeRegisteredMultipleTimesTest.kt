package de.halfbit.comachine.tests

import de.halfbit.comachine.mutableComachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals

class DelegateOnExitCanBeRegisteredMultipleTimesTest {

    sealed interface State {
        object One : State
        object Two : State
    }

    @Test
    fun test() {

        val machine = mutableComachine<State, Unit>(startWith = State.One)

        var calledCounter = 0

        machine.registerDelegate {
            whenIn<State.One> {
                onExit {
                    calledCounter += 1
                }
            }
        }

        machine.registerDelegate {
            whenIn<State.One> {
                onExit {
                    calledCounter += 1
                }
            }
        }

        machine.registerDelegate {
            whenIn<State.One> {
                onEnter { transitionTo(State.Two) }
                onExit {
                    calledCounter += 1
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
                expected = 3,
                actual = calledCounter
            )
        }
    }
}