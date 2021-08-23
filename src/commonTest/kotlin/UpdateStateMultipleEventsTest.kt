package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateStateMultipleEventsTest {

    data class State(
        val progress: Int = 0,
        val done: Int = 0,
    )

    object Event

    @Test
    fun test() {

        val machine = comachine<State, Event>(
            startWith = State()
        ) {
            whenIn<State> {
                onEnter {
                    launch {
                        for (i in 0..99) {
                            state.update { copy(progress = progress + 1) }
                            if (i % 5 == 0) {
                                yield()
                            }
                        }
                        state.update { copy(done = done + 1) }
                    }
                }
                onConcurrent<Event> {
                    for (i in 0..9) {
                        state.update { copy(progress = progress + 1) }
                        if (i % 2 == 0) {
                            yield()
                        }
                    }
                    state.update { copy(done = done + 1) }
                }
            }
        }

        executeBlockingTest {
            machine.startInScope(this)

            for (i in 0..9) {
                machine.send(Event)
            }

            machine.await<State> { done == 11 }
            val state = machine.state.first()

            assertEquals(11, state.done)
            assertEquals(200, state.progress)
        }
    }
}