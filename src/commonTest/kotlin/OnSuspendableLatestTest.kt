package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnSuspendableLatestTest {

    data class State(
        val counter: Int = 0,
    )

    data class Event(val index: Int)

    @Test
    fun test() {

        val events = mutableListOf<Event>()
        var firstEventJob: Job? = null
        val eventOneReceived = CompletableDeferred<Unit>()
        val eventTwoReceived = CompletableDeferred<Unit>()
        val machine = comachine<State, Event>(startWith = State()) {
            whenIn<State> {
                onLatest<Event> { event ->
                    coroutineScope {
                        events.add(event)
                        if (event.index == 1) {
                            firstEventJob = launch { delay(1000) }
                            eventOneReceived.complete(Unit)
                            delay(1000)
                            events.add(event)
                        }

                        if (event.index == 2) {
                            eventTwoReceived.complete(Unit)
                        }
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
            withTimeout(1000) {
                eventOneReceived.await()
            }
            machine.send(Event(index = 2))
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

            assertTrue(
                actual = firstEventJob?.isCancelled == true,
                message = "Expect first event's job to be cancelled"
            )
        }
    }
}