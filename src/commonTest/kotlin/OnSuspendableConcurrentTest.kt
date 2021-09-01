package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.Test

class OnSuspendableConcurrentTest {

    data class State(
        val counter: Int = 0,
    )

    data class Event(val index: Int)

    @Test
    fun test() {

        val first = CompletableDeferred<Unit>()
        val second = CompletableDeferred<Unit>()
        
        val machine = comachine<State, Event>(startWith = State()) {
            whenIn<State> {
                onConcurrent<Event> { event ->
                    when (event.index) {
                        1 -> first.complete(Unit)
                        2 -> second.complete(Unit)
                    }
                    delay(1000)
                    error("delay wasn't cancelled in time")
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
            machine.send(Event(index = 1))
            machine.send(Event(index = 2))

            first.await()
            second.await()
        }
    }
}