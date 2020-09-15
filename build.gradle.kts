import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Date

plugins {
    kotlin("jvm") version "1.4.10"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4"
}

val artifactName = "es-apm-extension"
val artifactGroup = "kr.jadekim"
val artifactVersion = "0.0.3"
group = artifactGroup
version = artifactVersion

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    val jLoggerVersion: String by project
    val esApmVersion: String by project
    val ktorExtensionVersion: String by project
    val exposedExtensionVersion: String by project

    api("co.elastic.apm:apm-agent-api:$esApmVersion")

    compileOnly("kr.jadekim:j-logger:$jLoggerVersion")
    compileOnly("kr.jadekim:ktor-extension:$ktorExtensionVersion")
    compileOnly("kr.jadekim:exposed-extension:$exposedExtensionVersion")
}

tasks.withType<KotlinCompile> {
    val jvmTarget: String by project

    kotlinOptions.jvmTarget = jvmTarget
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

publishing {
    publications {
        create<MavenPublication>("lib") {
            groupId = artifactGroup
            artifactId = artifactName
            version = artifactVersion
            from(components["java"])
            artifact(sourcesJar)
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")

    publish = true

    setPublications("lib")

    pkg.apply {
        repo = "maven"
        name = rootProject.name
        setLicenses("MIT")
        setLabels("kotlin")
        vcsUrl = "https://github.com/jdekim43/es-apm-extension.git"
        version.apply {
            name = artifactVersion
            released = Date().toString()
        }
    }
}