![maintenance-status](https://img.shields.io/badge/maintenance-experimental-blue.svg)

# Comachine

<img src="https://github.com/beworker/comachine/blob/master/docs/comachine.png" />

# Features

- **Kotlin corutines.** Event handlers can launch coroutines for collecting external events of performing side effects.
- **Structured concurrency.** Each states is launched in its own coroutine scope, called state scope. Coroutines running in a state scope will be properly cancelled on each state transition.
- **Suspendable event handlers.** Four built-in suspendable event handler types can simplify handling of events.
- **Decomposition by feature**. State machine can delegate declaration and handling of events to independent features.
- **State extras**. Fearures can store objects in the current state even if they are not a part of the state type.

# Examples

1. [Simple state machine](https://github.com/beworker/comachine/blob/master/src/commonTest/kotlin/AggregateStatesTest.kt)
2. [Delegation and code decomposition](https://github.com/beworker/comachine/blob/master/src/commonTest/kotlin/DelegateOnAnyCanBeRegisteredTest.kt)

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
    implementation 'de.halfbit:comachine-jvm:1.0-SNAPSHOT'
}
```
