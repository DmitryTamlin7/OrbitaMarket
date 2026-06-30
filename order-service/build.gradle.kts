plugins {
    id("java")
    id("org.springframework.boot") version "3.4.0"
    id("io.qameta.allure") version "3.0.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

springBoot {
    mainClass.set("org.example.OrderApplication")
}

val allureVersion = "2.24.0"
val restAssuredVersion = "5.3.2"
val junitVersion = "5.10.1"


dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.5"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.kafka:spring-kafka")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    testCompileOnly("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
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

tasks.getByName<Jar>("jar") {
    enabled = false
}

