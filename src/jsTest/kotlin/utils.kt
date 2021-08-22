package de.halfbit.comachine.tests

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun executeBlocking(block: suspend CoroutineScope.() -> Unit): dynamic {
    return GlobalScope.promise { block() }
}
