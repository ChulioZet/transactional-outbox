import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    id("java-library")
    id("io.freefair.lombok") version "6.5.1"
    id("com.google.protobuf") version "0.8.14"
    id("maven-publish")
    id("org.sonarqube") version "2.8"
    id("jacoco")
    id("com.github.hierynomus.license") version "0.16.1"
    id("signing")
}

val protobufVersion by extra("3.12.2")

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "com.google.protobuf")
    apply(plugin = "maven-publish")
    apply(plugin = "org.sonarqube")
    apply(plugin = "jacoco")
    apply(plugin = "com.github.hierynomus.license")
    apply(plugin = "signing")

    group = "one.tomorrow.transactional-outbox"

    java {
        withJavadocJar()
        withSourcesJar()
    }

    protobuf {
        protoc {
            artifact = "com.google.protobuf:protoc:$protobufVersion"
        }
    }

    sourceSets {
        test {
            java {
                // declared here so that the IDE knows this src dir
                srcDir("${project.buildDir}/generated/source/proto/test/java")
            }
        }
    }

    license {
        header = file("../LICENSE-header.txt")
        exclude("one/tomorrow/kafka/messages/DeserializerMessages.java") // java sources generated from proto messages
        include("**/*.java")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
        repositories {
            mavenLocal()
        }
    }

    // 'signing' has to be defined after/below 'publishing' so that it can reference the publication
    signing {
        val signingKeyId: String? by project
        val signingKey: String? by project
        useInMemoryPgpKeys(signingKeyId, signingKey, "")
        sign(publishing.publications["maven"])
    }

}

allprojects {

    repositories {
        mavenCentral()
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        testLogging {
            events(SKIPPED, PASSED, FAILED)
            showStandardStreams = false // change to true to get log output from tests
            exceptionFormat = FULL
        }

        finalizedBy("jacocoTestReport")
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.apply {
                isEnabled = true
                destination = File("build/reports/jacoco.xml")
            }
            executionData(tasks.withType<Test>())
        }
    }
}