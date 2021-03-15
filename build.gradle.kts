import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id( "com.github.ben-manes.versions") version "0.38.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.springframework.boot") version "2.4.3"
    kotlin("jvm") version "1.4.31"
    kotlin("kapt") version "1.4.31"
    kotlin("plugin.spring") version "1.4.31"
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
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("com.github.dwp:dataworks-common-logging:0.0.6")
    implementation("com.github.everit-org.json-schema:org.everit.json.schema:1.12.2")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("io.arrow-kt:arrow-core:0.11.0")
    implementation("io.arrow-kt:arrow-syntax:0.11.0")
    implementation("io.micrometer:micrometer-core:1.6.4")
    implementation("io.micrometer:micrometer-registry-prometheus:1.6.4")
    implementation("io.prometheus:simpleclient:0.10.0")
    implementation("io.prometheus:simpleclient_caffeine:0.10.0")
    implementation("io.prometheus:simpleclient_logback:0.10.0")
    implementation("io.prometheus:simpleclient_pushgateway:0.10.0")
    implementation("io.prometheus:simpleclient_spring_web:0.10.0")
    implementation("io.prometheus:simpleclient_httpserver:0.10.0")
    implementation("mysql:mysql-connector-java:6.0.6")
    implementation("org.apache.commons:commons-dbcp2:2.8.0")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("org.apache.kafka:kafka-clients:2.7.0")
    implementation("org.bouncycastle:bcprov-ext-jdk15on:1.68")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.retry:spring-retry")
    implementation("software.amazon.awssdk:kms:2.16.12")
    implementation("software.amazon.awssdk:secretsmanager:2.16.12")
    implementation("software.amazon.awssdk:ssm:2.16.12")

    kapt("io.arrow-kt:arrow-meta:0.11.0")

    testImplementation("com.nhaarman.mockitokotlin2", "mockito-kotlin", "2.2.0")
    testImplementation("io.kotest:kotest-assertions-arrow-jvm:4.4.1")
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.4.1")
    testImplementation("io.kotest:kotest-assertions-json-jvm:4.4.1")
    testImplementation("io.kotest:kotest-property-jvm:4.4.1")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.4.1")
    testImplementation("io.kotest:kotest-extensions-spring:4.4.1")
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
