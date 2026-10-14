plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "pe.ralvaro"
version = "0.0.1-SNAPSHOT"
description = "hotelya-back"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.15.0")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    testImplementation("org.springframework.security:spring-security-test")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Integration tests require HotelYa's local Supabase session pooler.
tasks.register<Test>("integrationTest") {
    description = "Tests JPA, Flyway and HTTP contracts through local Supabase's session pooler"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("integration") }
    shouldRunAfter(tasks.test)
}
tasks.test { useJUnitPlatform { excludeTags("integration") } }

jacoco { toolVersion = "0.8.15" }

// Measure business logic and authentication, independently of database/browser tests.
val unitCoverageClasses = sourceSets.main.get().output.asFileTree.matching {
    include("pe/ralvaro/hotelyaback/service/**", "pe/ralvaro/hotelyaback/security/**")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(unitCoverageClasses)
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    classDirectories.setFrom(unitCoverageClasses)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.70".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "0.50".toBigDecimal()
            }
        }
    }
}

tasks.test { finalizedBy(tasks.jacocoTestReport) }
tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }
