plugins {
    application
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

tasks.withType<JavaExec> {
    workingDir("run")
    standardInput = System.`in`
    standardOutput = System.out
    errorOutput = System.err
}

dependencies {
    implementation("com.discord4j:discord4j-core:3.2.7")
}