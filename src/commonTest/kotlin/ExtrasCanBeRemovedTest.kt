package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.Test

class ExtrasCanBeRemovedTest {

    data class State(val value: String? = "initial")

    @Test
    fun test() {

        val machine = comachine<State, Unit>(
            startWith = State()
        ) {
            whenIn<State> {
                onEnter {
                    setExtra<String?>("custom value")
                }
                on<Unit> {
                    removeExtra<String?>()
                    val value = getExtra<String?>()
                    state.update { copy(value = value) }
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
            machine.await<State> { value == null }
        }
    }
}