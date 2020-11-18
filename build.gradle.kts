import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.3.5.RELEASE"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    kotlin("jvm") version "1.4.10"
    kotlin("plugin.spring") version "1.4.10"
    kotlin("kapt") version "1.4.10"
}

group = "gov.dwp.dataworks.kafka"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://jitpack.io")
    maven("https://dl.bintray.com/arrow-kt/arrow-kt/")
}

dependencies {
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.4.1")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.apache.kafka:kafka-clients:2.3.0")
    implementation("com.github.dwp:dataworks-common-logging:0.0.6")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("org.bouncycastle:bcprov-ext-jdk15on:1.62")
    implementation("io.arrow-kt:arrow-core:0.11.0")
    implementation("io.arrow-kt:arrow-syntax:0.11.0")
    kapt("io.arrow-kt:arrow-meta:0.11.0")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.3.1")
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.3.1")
    testImplementation("io.kotest:kotest-assertions-arrow-jvm:4.3.1")
    testImplementation("io.kotest:kotest-assertions-json-jvm:4.3.1")
    testImplementation("io.kotest:kotest-property-jvm:4.3.1")
    testImplementation("com.nhaarman.mockitokotlin2", "mockito-kotlin", "2.2.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}


tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}
