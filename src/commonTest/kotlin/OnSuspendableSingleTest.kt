package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
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

class OnSuspendableSingleTest {

    data class State(val done: Boolean = false)
    data class Event(val index: Int)

    @Test
    fun followingEventsIgnoredWhileCurrentIsStillInProgressTest() {

        val events = mutableListOf<Event>()
        var firstEventJob: Job? = null
        val allEventsSent = CompletableDeferred<Unit>()

        val machine = comachine<State, Event>(startWith = State()) {
            whenIn<State> {
                onSingle<Event> { event ->
                    events.add(event)
                    coroutineScope {
                        if (event.index == 0) {
                            firstEventJob = launch { delay(1000) }
                            withTimeout(1000) {
                                allEventsSent.await()
                            }
                            state.update { copy(done = true) }
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
            repeat(10) {
                machine.send(Event(index = it))
            }
            allEventsSent.complete(Unit)
            machine.await<State> { done }

            assertEquals(
                expected = listOf(Event(index = 0)),
                actual = events
            )

            assertTrue(
                actual = firstEventJob?.isActive == true,
                message = "Expect first event's job to not be cancelled"
            )
        }
    }
}