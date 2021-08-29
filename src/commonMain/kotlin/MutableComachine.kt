package de.halfbit.comachine

import de.halfbit.comachine.dsl.ComachineDelegateBlock

interface MutableComachine<State : Any, Event : Any> : Comachine<State, Event> {
    fun registerDelegate(block: ComachineDelegateBlock<State, Event>.() -> Unit)
}
