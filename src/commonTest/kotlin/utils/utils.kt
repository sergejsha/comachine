package de.halfbit.comachine.tests.utils

import kotlinx.coroutines.CoroutineScope

expect fun executeBlocking(block: suspend CoroutineScope.() -> Unit)
