package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.test.Test

class ComachineTest {

    sealed interface State {
        data class Loading(
            val loadingProgress: Int = 0
        ) : State

        data class Ready(
            val payload: String,
            val playing: Boolean = false
        ) : State
    }

    sealed interface Event {
        data class SetPlaying(val playing: Boolean) : Event
    }

    object Actions {
        suspend fun loadAsset(): String {
            println("### loading asset...")
            delay(4000)
            return "pachelbel's canon"
        }
    }

    @Test
    fun test_Comachine() {

        val comachine = comachine<State, Event>(
            startWith = State.Loading()
        ) {

            whenIn<State.Loading> {
                onEnter {
                    val progressUpdate = launch {
                        while (true) {
                            state.update { copy(loadingProgress = loadingProgress + 1) }
                            delay(250)
                        }
                    }

                    launch {
                        val payload = Actions.loadAsset()
                        println("### asset loaded!")

                        progressUpdate.cancel()
                        state.update { copy(loadingProgress = 100) }
                        transitionTo { State.Ready(payload) }
                    }
                }

                onExit {
                    println("### loading completed")
                }
            }

            whenIn<State.Ready> {
                onEnter {
                    yield()
                    println("### Ready entered")
                }

                // onConcurrent<> {  }
                // onExclusive<> {  }
                // onLatest<> {  }
                // onSequential<> {  }

                onSequential<Event.SetPlaying> { event ->
                    delay(100)
                    state.update { copy(playing = event.playing) }
                }
            }
        }

        runBlockingTest {
            launch { comachine.state.collect { println("    ------ $it ------") } }

            launch { comachine.loop() }
            delay(5000)

            comachine.send(Event.SetPlaying(playing = true))
            comachine.send(Event.SetPlaying(playing = false))
            comachine.send(Event.SetPlaying(playing = true))
            comachine.send(Event.SetPlaying(playing = false))
            delay(4000)

            println("... cancelling")
            coroutineContext.cancelChildren()
        }
    }
}