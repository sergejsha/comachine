package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateStateSingleEventTest {

    data class State(
        val location: String = "location",
        val progress: Int = 0,
        val asset: String? = null,
    )

    object Actions {
        suspend fun loadAsset(location: String): String {
            yield()
            return "locomotive breath @$location"
        }
    }

    @Test
    fun test() {

        val machine = comachine<State, Unit>(
            startWith = State()
        ) {
            whenIn<State> {
                onEnter {
                    state.update { copy(progress = progress + 1) }

                    val updateCalled = CompletableDeferred<Unit>()
                    launch {
                        coroutineScope {
                            state.update { copy(progress = progress + 1) }
                            state.update { copy(progress = progress + 1) }
                            updateCalled.complete(Unit)
                        }
                    }

                    val asset = Actions.loadAsset(state.location)

                    updateCalled.await()
                    state.update { copy(asset = asset) }
                }
            }
        }

        executeBlockingTest {

            val states = mutableListOf<State>()
            val job = launch {
                machine.state.collect { states.add(it) }
            }

            machine.startInScope(this)
            machine.await<State> { asset != null }
            job.cancel()

            assertEquals(
                listOf(
                    State(progress = 0),
                    State(progress = 1),
                    State(progress = 2),
                    State(progress = 3),
                    State(progress = 3, asset = "locomotive breath @location"),
                ),
                states
            )
        }
    }
}