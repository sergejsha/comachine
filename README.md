# Comachine
Finite-State Machine for Kotlin coroutines (multiplatform).

The project is in early experimental stage, changes to the API are still possible.

* Can I use it in production: No
* Can I try it out in my not published app: Sure

# Usage
```kotlin
// define states and events
sealed interface State {
    data class Solid(val progress: String = "") : State
    data class Liquid(val progress: String = "") : State
    data class Gas(val progress: String = "") : State
}

sealed interface Event {
    object OnHeat : Event
    object OnCold : Event
}

// define the state machine
val comachine = comachine<State, Event>(
    startWith = State.Solid()
) {
    whenIn<State.Solid> {
        onExclusive<Event.OnHeat> {
            state.update { copy(progress = "melting...") }
            delay(100)
            state.update { copy(progress = "melted") }
            transitionTo { State.Liquid() }
        }
    }

    whenIn<State.Liquid> {
        onExclusive<Event.OnHeat> {
            state.update { copy(progress = "vaporizing...") }
            delay(100)
            state.update { copy(progress = "vaporized") }
            transitionTo { State.Gas() }
        }
        onExclusive<Event.OnCold> {
            state.update { copy(progress = "freezing...") }
            delay(100)
            state.update { copy(progress = "frozen") }
            transitionTo { State.Solid() }
        }
    }

    whenIn<State.Gas> {
        onExclusive<Event.OnCold> {
            state.update { copy(progress = "condensing...") }
            delay(100)
            state.update { copy(progress = "condensed") }
            transitionTo { State.Liquid() }
        }
    }
}

// run the state machine
runBlockingTest {
    launch { comachine.state.collect { println("------ $it") } }
    comachine.startInScope(this)

    println("OnHeat")
    comachine.send(Event.OnHeat)
    comachine.await<State.Liquid>()

    println("OnHeat")
    comachine.send(Event.OnHeat)
    comachine.await<State.Gas>()

    println("OnCold")
    comachine.send(Event.OnCold)
    comachine.await<State.Liquid>()

    println("OnCold")
    comachine.send(Event.OnCold)
    comachine.await<State.Solid>()

    coroutineContext.cancelChildren()
}

// output
------ Solid(progress=)
OnHeat
------ Solid(progress=melting...)
------ Solid(progress=melted)
------ Liquid(progress=)
OnHeat
------ Liquid(progress=vaporizing...)
------ Liquid(progress=vaporized)
------ Gas(progress=)
OnCold
------ Gas(progress=condensing...)
------ Gas(progress=condensed)
------ Liquid(progress=)
OnCold
------ Liquid(progress=freezing...)
------ Liquid(progress=frozen)
------ Solid(progress=)
```

# Binaries
```groovy
// in project build file
allprojects {
    repositories {
        maven { url = "https://oss.sonatype.org/content/repositories/snapshots/" }
    }
}

// in module build file
dependencies {
    implementation 'de.halfbit:comachine-jvm:0.1-SNAPSHOT'
}
```
