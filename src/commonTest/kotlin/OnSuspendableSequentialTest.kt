package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals

class OnSuspendableSequentialTest {

    data class State(val count: Int = 0)
    data class Event(val index: Int)

    @Test
    fun newEventsAreScheduledForExecutionAfterTheCurrentOneIsDone() {

        val events = mutableListOf<Event>()
        val allEventsSent = CompletableDeferred<Unit>()

        val machine = comachine<State, Event>(startWith = State()) {
            whenIn<State> {
                onSequential<Event> { event ->
                    events.add(event)
                    state.update { copy(count = count + event.index) }
                    allEventsSent.await()
                    events.add(event)
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
            machine.await<State> { count == 45 }

            assertEquals(
                expected = listOf(
                    Event(index = 0),
                    Event(index = 0),
                    Event(index = 1),
                    Event(index = 1),
                    Event(index = 2),
                    Event(index = 2),
                    Event(index = 3),
                    Event(index = 3),
                    Event(index = 4),
                    Event(index = 4),
                    Event(index = 5),
                    Event(index = 5),
                    Event(index = 6),
                    Event(index = 6),
                    Event(index = 7),
                    Event(index = 7),
                    Event(index = 8),
                    Event(index = 8),
                    Event(index = 9),
                    Event(index = 9),
                ),
                actual = events
            )
        }
    }
}