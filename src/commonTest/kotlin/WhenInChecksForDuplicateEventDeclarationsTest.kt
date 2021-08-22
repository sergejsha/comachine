package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import kotlin.test.Test
import kotlin.test.fail

class WhenInChecksForDuplicateEventDeclarationsTest {

    object State
    object Event

    @Test()
    fun test() {
        try {
            comachine<State, Event>(startWith = State) {
                whenIn<State> {
                    onSequential<Event> {}
                    onLatest<Event> {}
                }
            }
        } catch (actual: IllegalArgumentException) {
            return
        }
        fail("IllegalArgumentException is expected")
    }
}