package de.halfbit.comachine.tests

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

@DelicateCoroutinesApi
actual fun runBlockingTest(block: suspend CoroutineScope.() -> Unit): dynamic {
    return GlobalScope.promise { block() }
}
