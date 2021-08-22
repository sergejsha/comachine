package de.halfbit.comachine.tests

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren

fun executeBlockingTest(block: suspend CoroutineScope.() -> Unit) {
    executeBlocking {
        block()
        coroutineContext.cancelChildren()
    }
}
