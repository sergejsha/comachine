package de.halfbit.comachine.tests

import de.halfbit.comachine.Comachine
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.time.Duration

suspend inline fun <reified State : Any> Comachine<*, *>.await(
    timeout: Duration = Duration.milliseconds(3000)
) {
    runCatching {
        coroutineScope {
            launch {
                delay(timeout)
                cancel("timeout")
            }
            state.collect {
                if (it::class == State::class) {
                    cancel("state detected")
                }
            }
        }
    }
}