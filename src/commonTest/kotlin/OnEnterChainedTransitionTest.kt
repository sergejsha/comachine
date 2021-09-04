package de.halfbit.comachine.tests

import de.halfbit.comachine.comachine
import de.halfbit.comachine.startInScope
import de.halfbit.comachine.tests.utils.await
import de.halfbit.comachine.tests.utils.executeBlockingTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals

class OnEnterChainedTransitionTest {

    sealed interface State {
        object Zero : State
        object One : State
        object Two : State
        object Three : State
        object Four : State
        object Five : State
        object Six : State
        object Seven : State
        object Eight : State
        object Nine : State
        object Ten : State
    }

    @Test
    fun test() {

        val machine = comachine<State, Unit>(
            startWith = State.Zero
        ) {
            whenIn<State.Zero> { onEnter { transitionTo { State.One } } }
            whenIn<State.One> { onEnter { transitionTo { State.Two } } }
            whenIn<State.Two> { onEnter { transitionTo { State.Three } } }
            whenIn<State.Three> { onEnter { transitionTo { State.Four } } }
            whenIn<State.Four> { onEnter { transitionTo { State.Five } } }
            whenIn<State.Five> { onEnter { transitionTo { State.Six } } }
            whenIn<State.Six> { onEnter { transitionTo { State.Seven } } }
            whenIn<State.Seven> { onEnter { transitionTo { State.Eight } } }
            whenIn<State.Eight> { onEnter { transitionTo { State.Nine } } }
            whenIn<State.Nine> { onEnter { transitionTo { State.Ten } } }
            whenIn<State.Ten> { }
        }

        executeBlockingTest {
            val states = mutableListOf<State>()
            launch {
                machine.state.collect {
                    states += it
                }
            }

            machine.startInScope(this)
            machine.await<State.Ten>()

            assertEquals(
                states,
                listOf(
                    State.Zero,
                    State.One,
                    State.Two,
                    State.Three,
                    State.Four,
                    State.Five,
                    State.Six,
                    State.Seven,
                    State.Eight,
                    State.Nine,
                    State.Ten,
                )
            )
        }
    }
}