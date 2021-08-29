package de.halfbit.comachine.tests.utils

import de.halfbit.comachine.Comachine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.time.Duration

suspend inline fun <reified State : Any> Comachine<*, *>.await(
    timeout: Duration = Duration.milliseconds(5000)
) {
    await<State>(timeout) { this::class == State::class }
}

suspend inline fun <reified State : Any> Comachine<*, *>.await(
    timeout: Duration = Duration.milliseconds(3000),
    crossinline block: State.() -> Boolean
) {
    try {
        coroutineScope {
            launch {
                delay(timeout)
                error("timeout")
            }
            state.collect {
                if (it is State && block(it)) {
                    cancel("state detected")
                }
            }
        }
    } catch (err: CancellationException) {
        // all good
    }
}