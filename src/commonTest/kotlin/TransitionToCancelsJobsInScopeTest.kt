package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertTrue

class TransitionToCancelsJobsInScopeTest {

    sealed interface State {
        data class Loading(val progress: Int = 0) : State
        object Ready : State
    }

    object Event

    @Test
    fun test() {

        var cancelled = false

        val machine = comachine<State, Event>(
            startWith = State.Loading()
        ) {
            whenIn<State.Loading> {
                onEnter {
                    launch {
                        try {
                            while (true) {
                                state.update { copy(progress = progress + 1) }
                            }
                        } catch (err: CancellationException) {
                            cancelled = true
                            throw err
                        }
                    }
                }
                onSequential<Event> {
                    transitionTo { State.Ready }
                }
            }
        }

        executeBlockingTest {

            machine.startInScope(this)
            machine.await<State.Loading> { progress > 10 }

            machine.send(Event)
            machine.await<State.Ready>()

            assertTrue(cancelled)
        }
    }
}