# Comachine
Finite-State Machine for Kotlin coroutines

**Status:** experimental

# Usage
```kotlin
// define states and events
sealed interface State {
    object Solid : State
    object Liquid : State
    object Gas : State
}


sealed interface Event {
    object OnHeat : Event
    object OnCold : Event
}

// define state machine
val comachine = comachine<State, Event>(
    startWith = State.Solid
) {
    whenIn<State.Solid> {
        onExclusive<Event.OnHeat> {
            println("melting...")
            delay(500)
            println("melted")
            transitionTo { State.Liquid }
        }
    }

    whenIn<State.Liquid> {
        onExclusive<Event.OnHeat> {
            println("vaporizing...")
            delay(500)
            println("vaporized")
            transitionTo { State.Gas }
        }
        onExclusive<Event.OnCold> {
            println("freezing...")
            delay(500)
            println("frozen")
            transitionTo { State.Solid }
        }
    }

    whenIn<State.Gas> {
        onExclusive<Event.OnCold> {
            println("condensing...")
            delay(500)
            println("condensed")
            transitionTo { State.Liquid }
        }
    }
}

// run state machine
runBlockingTest {
    launch {
        comachine.state.collect {
            println("    ------ ${it::class.simpleName} ------")
        }
    }
    launch { comachine.loop() }
    delay(500)

    println("OnHeat")
    comachine.send(Event.OnHeat)
    delay(1000)

    println("OnHeat")
    comachine.send(Event.OnHeat)
    delay(1000)

    println("OnCold")
    comachine.send(Event.OnCold)
    delay(1000)

    println("OnCold")
    comachine.send(Event.OnCold)
    delay(1000)

    coroutineContext.cancelChildren()
}

// output
------ Solid ------
OnHeat
melting...
melted
------ Liquid ------
OnHeat
vaporizing...
vaporized
------ Gas ------
OnCold
condensing...
condensed
------ Liquid ------
OnCold
freezing...
frozen
------ Solid ------
```

# Binaries
```groovy
// in project build file
allprojects {
    repositories {
        mavenCentral()
        maven { url = "https://oss.sonatype.org/content/repositories/snapshots/" }
    }
}

// in module build file
dependencies {
    implementation 'de.halfbit:comachine-jvm:1.0-SNAPSHOT'
}
```
