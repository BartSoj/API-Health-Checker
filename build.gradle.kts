plugins {
    kotlin("jvm") version "2.1.10"
    application
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.5.17")
    implementation("io.swagger.parser.v3:swagger-parser:2.1.25")
    implementation("io.ktor:ktor-client-core:3.1.1")
    implementation("io.ktor:ktor-client-cio:3.1.1")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}