plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0" // JavaFX Plugin
}

group = "com.yourorg.telemetryagent"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Define versions for our libraries in one place
val oshiVersion = "6.6.1"
val javafxVersion = "21.0.2"
val sqliteJdbcVersion = "3.46.0.0"
val jacksonVersion = "2.17.1"
val slf4jVersion = "2.0.13"
val logbackVersion = "1.5.6"
val jnaVersion = "5.14.0"
val junitVersion = "5.10.2"

dependencies {
    // --- Core Libraries ---
    implementation("com.github.oshi:oshi-core:$oshiVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteJdbcVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")

    // --- Logging ---
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // --- Windows DPAPI for Security (Optional but good practice) ---
    implementation("net.java.dev.jna:jna:$jnaVersion")
    implementation("net.java.dev.jna:jna-platform:$jnaVersion")

    // --- Testing ---
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

application {
    // Define the main class that starts our application
    mainClass.set("com.yourorg.telemetryagent.app.TelemetryApplication")
}

javafx {
    version = javafxVersion
    // We need these JavaFX modules for our UI
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics", "javafx.web")
}

tasks.test {
    useJUnitPlatform()
}