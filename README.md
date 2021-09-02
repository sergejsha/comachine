# Comachine

<img src="https://github.com/beworker/comachine/blob/master/docs/comachine.png" />

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
