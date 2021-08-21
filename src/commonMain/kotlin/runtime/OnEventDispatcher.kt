package de.halfbit.comachine.runtime

internal interface OnEventDispatcher<SubEvent : Any> {
    fun onEventReceived(event: SubEvent)
    fun onEventCompleted(event: SubEvent)
}
