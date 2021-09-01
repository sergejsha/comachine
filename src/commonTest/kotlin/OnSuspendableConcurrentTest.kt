package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class OnSuspendableConcurrentTest {

    data class State(val counter: Int = 0)
    data class Event(val index: Int)

    @Test
    fun newEventsAreProcessedConcurrentlyToCurrentEvents() {

        val events = mutableListOf<Event>()
        val secondBucketSent = CompletableDeferred<Unit>()

        val machine = comachine<State, Event>(startWith = State()) {
            whenIn<State> {
                onConcurrent<Event> { event ->
                    if (event.index < 5) {
                        withTimeout(1000) {
                            secondBucketSent.await()
                        }
                    }
                    events.add(event)
                    state.update { copy(counter = counter + event.index) }
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

            launch {
                repeat(5) {
                    machine.send(Event(index = it))
                }
            }

            launch {
                repeat(5) {
                    machine.send(Event(index = 5 + it))
                }
                secondBucketSent.complete(Unit)
            }

            machine.await<State> { counter == 45 }
            assertEquals(expected = events.size, actual = 10)
        }
    }
}