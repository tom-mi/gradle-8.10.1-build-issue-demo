import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "2.0.20"
    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.kapt") version kotlinVersion
    id("com.google.cloud.tools.jib") version "3.4.3"
    id("org.owasp.dependencycheck") version "10.0.4"
    id("com.github.hierynomus.license-report") version "0.16.1"
    id("com.github.ben-manes.versions") version "0.51.0"
}

group = "foo"
version = "0.0.1-SNAPSHOT"
kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.AMAZON
    }
}

repositories {
    mavenCentral()
    maven("https://s3.eu-central-1.amazonaws.com/dynamodb-local-frankfurt/release")
}

val awsSdkVersion = "1.12.772"
val awsSdkV2Version = "2.27.22"
val datadogApmVersion = "1.39.0"
val springCloudVersion = "2023.0.3"
val springCloudAws = "3.1.1"

dependencies {
    implementation("com.amazonaws:aws-java-sdk-dynamodb:$awsSdkVersion")
    implementation("com.datadoghq:dd-trace-api:$datadogApmVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")
    implementation("io.micrometer:micrometer-registry-cloudwatch2")
    implementation("io.micrometer:micrometer-registry-statsd")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("software.amazon.awssdk:apache-client:$awsSdkV2Version")
    implementation("io.github.resilience4j:resilience4j-spring-boot2:2.2.0")

    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion"))
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:$springCloudAws"))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-parameter-store")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.name.startsWith("snappy-java")) {
                useVersion("1.1.10.4")
                because("CVE-2023-34453,CVE-2023-34454,CVE-2023-34455,CVE-2023-43642")
            }
        }
    }
}

kapt {
    includeCompileClasspath = false
}


tasks {

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = false
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
            events("skipped", "failed", "passed")
        }
    }

    withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    downloadLicenses {
        includeProjectDependencies = true
        dependencyConfiguration = "runtimeClasspath"
    }

    test {
        useJUnitPlatform()
        maxHeapSize = "4g"
        jvmArgs = listOf("--add-opens", "java.base/java.time=ALL-UNNAMED", "--add-opens", "java.base/java.util=ALL-UNNAMED")
    }

    jib {
        from {
            image = "public.ecr.aws/amazoncorretto/amazoncorretto:21"
            credHelper.helper = "ecr-login"
        }
        container {
            ports = listOf("443")
        }
        to {
            image = "foo:bar"
        }
    }

}

