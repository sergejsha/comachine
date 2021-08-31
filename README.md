# Comachine
Finite-State Machine for Kotlin coroutines (multiplatform).

The library is [still in development](https://github.com/beworker/comachine/issues/1). Some minor changes to API are still possible.

# Examples

1. [Simple state machine](https://github.com/beworker/comachine/blob/master/src/commonTest/kotlin/AggregateStatesTest.kt)
2. [Delegation and code decomposition](https://github.com/beworker/comachine/blob/master/src/commonTest/kotlin/OnEventDelegateTest.kt)

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
