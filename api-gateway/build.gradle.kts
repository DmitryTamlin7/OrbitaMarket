plugins {
    java
    id("org.springframework.boot") version "3.2.5"
    id("io.qameta.allure") version "3.0.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val allureVersion = "2.24.0"
val restAssuredVersion = "5.3.2"
val junitVersion = "5.10.1"

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.5"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2023.0.1"))
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.qameta.allure:allure-junit5:2.25.0")
}

allure {
    version.set(allureVersion)
    adapter {
        frameworks {
            junit5 {
                adapterVersion.set(allureVersion)
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("allure.results.directory", layout.buildDirectory.dir("allure-results").get().asFile.absolutePath)
}