package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlocking
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
            val asset: String,
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
            return "Dave Brubeck - Take Five"
        }
    }

    @Test
    fun test_Comachine() {

        val comachine = comachine<State, Event>(
            startWith = State.Loading()
        ) {

            whenIn<State.Loading> {
                onEnter {
                    yield()
                    println("### Loading entered")

                    val progressUpdate = launch {
                        while (true) {
                            state.update { copy(loadingProgress = loadingProgress + 1) }
                            delay(250)
                        }
                    }

                    val asset = Actions.loadAsset()
                    println("### asset loaded!")

                    progressUpdate.cancel()
                    state.update { copy(loadingProgress = 100) }
                    transitionTo { State.Ready(asset) }
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

        executeBlocking {
            launch {
                comachine.state.collect {
                    println("    ------ $it")
                }
            }
            comachine.startInScope(this)

            comachine.await<State.Ready>()
            comachine.send(Event.SetPlaying(playing = true))
            comachine.send(Event.SetPlaying(playing = false))
            comachine.send(Event.SetPlaying(playing = true))
            comachine.send(Event.SetPlaying(playing = false))
            delay(1000)

            println("... cancelling")
            coroutineContext.cancelChildren()
        }
    }
}