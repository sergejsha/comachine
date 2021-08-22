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

class UpdateStateFromSingleEventHandler {

    data class Loading(
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

        val machine = comachine<Loading, Unit>(
            startWith = Loading()
        ) {
            whenIn<Loading> {
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

            val states = mutableListOf<Loading>()
            val job = launch {
                machine.state.collect { states.add(it) }
            }

            machine.startInScope(this)
            machine.await<Loading> { asset != null }
            job.cancel()

            assertEquals(
                listOf(
                    Loading(progress = 0),
                    Loading(progress = 1),
                    Loading(progress = 2),
                    Loading(progress = 3),
                    Loading(progress = 3, asset = "locomotive breath @location"),
                ),
                states
            )
        }
    }
}