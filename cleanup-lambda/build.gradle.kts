import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
    kotlin("jvm")
    id("com.github.hierynomus.license-report")
    id("com.github.ben-manes.versions")
    id("org.owasp.dependencycheck")
    id("com.google.osdetector") version "1.7.3"
}

group = "foo.cleanuplambda"
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

dependencies {
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.13.0")
    implementation("com.amazonaws:aws-java-sdk-dynamodb:1.12.772")

    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.24.0")

    implementation("com.amazonaws:aws-lambda-java-log4j2:1.6.0")
    implementation("com.datadoghq:java-dogstatsd-client:4.4.2")

    testImplementation("com.amazonaws:DynamoDBLocal:2.5.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
    if (osdetector.os == "osx" && osdetector.arch == "aarch_64") {
        testImplementation("io.github.ganadist.sqlite4java:libsqlite4java-osx-aarch64:1.0.392")
    }
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
}

tasks {

    withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    withType<Test> {
        useJUnitPlatform()
        jvmArgs = listOf("--add-opens", "java.base/java.time=ALL-UNNAMED", "--add-opens", "java.base/java.util=ALL-UNNAMED")
    }

}

tasks.register<Zip>("distribution") {
    from(tasks.compileKotlin)
    from(tasks.processResources)
    into("lib") {
        from(configurations.runtimeClasspath)
    }
}
tasks.build {
    dependsOn("distribution")
}

