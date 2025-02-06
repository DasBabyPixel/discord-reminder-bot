plugins {
    application
    id("com.gradleup.shadow") version "9.0.0-beta7"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

application {
    mainClass = "ReminderBot"
}

tasks.withType<JavaExec>().configureEach {
    workingDir("run")
    standardInput = System.`in`
    standardOutput = System.out
    errorOutput = System.err
}

dependencies {
    implementation("com.discord4j:discord4j-core:3.2.7")
    implementation("com.google.code.gson:gson:2.11.0")
}