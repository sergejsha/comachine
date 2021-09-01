package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

class TransitionToMovesToNewStateTest {

    sealed interface State {
        data class Loading(
            val location: String = "location1",
            val startPlaying: Boolean = false,
        ) : State

        data class Ready(
            val asset: String,
            val playing: Boolean,
        ) : State
    }

    sealed interface Event {
        data class Load(val location: String) : Event
        data class Playing(val playing: Boolean) : Event
    }

    object Actions {
        suspend fun loadAsset(
            location: String,
            loadingCompleted: CompletableDeferred<Unit>
        ): String {
            yield()
            return when (location) {
                "location1" -> {
                    loadingCompleted.await()
                    "asset1"
                }
                "location2" -> "asset2"
                else -> error("unknown location")
            }
        }

        suspend fun setPlaying(playing: Boolean) {
            yield()
        }

        suspend fun play(asset: String, playing: Boolean) {
            yield()
        }
    }

    @Test
    fun test() {

        val machine = comachine<State, Event>(
            startWith = State.Loading()
        ) {
            whenIn<State.Loading> {
                val loadingCompleted = CompletableDeferred<Unit>()
                onEnter {
                    launchInState {
                        val asset = Actions.loadAsset(state.location, loadingCompleted)
                        transitionTo { State.Ready(asset, startPlaying) }
                    }
                }
                onSequential<Event.Playing> { event ->
                    state.update { copy(startPlaying = event.playing) }
                    loadingCompleted.complete(Unit)
                }
            }
            whenIn<State.Ready> {
                onEnter {
                    launchInState {
                        Actions.play(state.asset, state.playing)
                    }
                }
                onSequential<Event.Playing> { event ->
                    Actions.setPlaying(event.playing)
                    state.update { copy(playing = event.playing) }
                }
                onLatest<Event.Load> { event ->
                    Actions.setPlaying(false)
                    transitionTo { State.Loading(event.location) }
                }
            }
        }

        executeBlockingTest {

            val states = mutableListOf<State>()
            val job = launch {
                machine.state.collect { states.add(it) }
            }

            machine.startInScope(this)

            machine.send(Event.Playing(playing = true))
            machine.await<State.Ready>()

            machine.send(Event.Playing(playing = false))
            machine.send(Event.Load(location = "location2"))
            machine.await<State.Loading>()
            machine.await<State.Ready>()

            job.cancel()

            assertEquals(
                listOf(
                    State.Loading(location = "location1", startPlaying = false),
                    State.Loading(location = "location1", startPlaying = true),
                    State.Ready(asset = "asset1", playing = true),
                    State.Ready(asset = "asset1", playing = false),
                    State.Loading(location = "location2", startPlaying = false),
                    State.Ready(asset = "asset2", playing = false),
                ),
                states
            )
        }
    }
}