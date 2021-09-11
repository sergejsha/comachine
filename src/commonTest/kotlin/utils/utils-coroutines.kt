package de.halfbit.comachine.tests.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.withTimeout

fun executeBlockingTest(block: suspend CoroutineScope.() -> Unit) {
    executeBlocking {
        withTimeout(3000) {
            block()
        }
        coroutineContext.cancelChildren()
    }
}
