package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals

class ExtrasAreEmptyInNewStateTest {

    sealed interface State {
        object One : State
        data class Two(val value: String? = "initial") : State
        data class Three(val value: String? = "initial") : State
        data class Four(val value: String? = "initial") : State
    }

    @Test
    fun test() {

        val machine = comachine<State, Unit>(
            startWith = State.One
        ) {
            whenIn<State.One> {
                onEnter {
                    setExtra<String?>("from state one")
                    val value = getExtra<String?>()
                    transitionTo { State.Two(value = value) }
                }
            }
            whenIn<State.Two> {
                onEnter {
                    val value = getExtra<String?>()
                    setExtra("from state two")
                    transitionTo { State.Three(value = value) }
                }
            }
            whenIn<State.Three> {
                onEnter {
                    val value = getExtra<String?>()
                    setExtra("form state three")
                    transitionTo { State.Four(value = value) }
                }
            }
            whenIn<State.Four> {
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
            machine.send(Unit)
            machine.await<State.Four>()

            assertEquals(
                expected = listOf(
                    State.One,
                    State.Two(value = "from state one"),
                    State.Three(value = null),
                    State.Four(value = null),
                ),
                actual = states
            )
        }
    }
}