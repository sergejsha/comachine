package de.halfbit.comachine.dsl

import kotlinx.coroutines.Job
import kotlin.reflect.KClass

private typealias LaunchInMachine = (suspend () -> Unit) -> Job

@ComachineDsl
class OnExitBlock<SubState : Any>
@PublishedApi
internal constructor(
    private val getStateFct: () -> SubState,
    @PublishedApi internal val extras: Lazy<MutableMap<KClass<*>, Any?>>,
    private val launchInMachineFct: LaunchInMachine,
) {

    val state: SubState
        get() = getStateFct()

    inline fun <reified T> getExtra(): T {
        return extras.value[T::class] as T
    }

    inline fun <reified T> removeExtra() {
        extras.value.remove(T::class)
    }

    inline fun <reified T> hasExtra(): Boolean {
        return extras.value.containsKey(T::class)
    }

    fun launchInMachine(block: suspend () -> Unit) {
        launchInMachineFct(block)
    }
}
