package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.Test

class ExtrasSetAndGetInSameStateTest {

    data class State(val value: String = "")

    @Test
    fun test() {

        val machine = comachine<State, Unit>(
            startWith = State()
        ) {
            whenIn<State> {
                onEnter {
                    setExtra("custom value")
                }
                on<Unit> {
                    val value = getExtra<String>()
                    state = state.copy(value = value)
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
            machine.send(Unit)
            machine.await<State> { value == "custom value" }
        }
    }
}