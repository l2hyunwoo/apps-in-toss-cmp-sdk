plugins {
    alias(libs.plugins.kotlin.multiplatform)
    `maven-publish`
}
group = "io.github.l2hyunwoo"
version = "0.1.0-SNAPSHOT"
kotlin {
    jvm()
    js { browser() }
    wasmJs { browser() }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.coroutines.core)
            implementation(libs.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }
    }
}
val webBridgeResourcesJar = tasks.register<Jar>("webBridgeResourcesJar") {
    archiveClassifier.set("web-resources")
    from("src/webMain/resources")
}
configurations.create("webBridgeResources") {
    isCanBeConsumed = true
    isCanBeResolved = false
    outgoing.artifact(webBridgeResourcesJar)
}

publishing {
    repositories {
        maven {
            name = "Template"
            url = uri(rootProject.layout.buildDirectory.dir("template-maven"))
        }
    }
    publications.withType<MavenPublication>().configureEach {
        artifactId = if (name == "kotlinMultiplatform") "apps-in-toss-sdk" else "apps-in-toss-sdk-${name.lowercase()}"
        if (name == "kotlinMultiplatform") artifact(webBridgeResourcesJar)
    }
}
