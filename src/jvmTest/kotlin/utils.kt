package de.halfbit.comachine.tests.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun executeBlocking(block: suspend CoroutineScope.() -> Unit) {
    runBlocking { block() }
}