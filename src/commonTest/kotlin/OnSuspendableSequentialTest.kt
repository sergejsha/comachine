package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class OnSuspendableSequentialTest {

    data class State(
        val counter: Int = 0,
    )

    data class Event(val index: Int)

    @Test
    fun test() {

        val events = mutableListOf<Event>()
        val eventTwoSent = CompletableDeferred<Unit>()
        val eventTwoReceived = CompletableDeferred<Unit>()

        val machine = comachine<State, Event>(startWith = State()) {
            whenIn<State> {
                onSequential<Event> { event ->
                    if (event.index == 1) {
                        eventTwoSent.await()
                    }
                    events.add(event)
                    if (event.index == 2) {
                        eventTwoReceived.complete(Unit)
                    }
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
            eventTwoSent.complete(Unit)

            withTimeout(1000) {
                eventTwoReceived.await()
            }

            assertEquals(
                expected = listOf(
                    Event(index = 1),
                    Event(index = 2)
                ),
                actual = events
            )
        }
    }
}