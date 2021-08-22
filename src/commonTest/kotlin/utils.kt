package de.halfbit.comachine.tests

import kotlinx.coroutines.CoroutineScope

expect fun executeBlocking(block: suspend CoroutineScope.() -> Unit)
