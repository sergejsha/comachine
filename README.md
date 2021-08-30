# Comachine
Finite-State Machine for Kotlin coroutines (multiplatform).

Work in progress. Some changes to API are possible.

# Examples

1. [Simple state machine](https://github.com/beworker/comachine/blob/7913cd58cf05eb015da263a00fa5ccf9671bc7e0/src/commonTest/kotlin/AggregateStatesTest.kt)
2. [Mutable state machine and code decomposition](src/commonTest/kotlin/DelegateCanRegisterWhenNotLoopingTest.kt)

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
