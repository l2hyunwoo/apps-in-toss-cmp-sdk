plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
}
group = "io.github.l2hyunwoo"
version = providers.gradleProperty("releaseVersion").orElse(providers.gradleProperty("sdkVersion")).get()
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

mavenPublishing {
    publishToMavenCentral()
    pom {
        name.set("Apps in Toss Compose Multiplatform SDK")
        description.set("Bring CMP to Apps in Toss")
        url.set("https://github.com/l2hyunwoo/apps-in-toss-cmp-sdk")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
            }
        }
        developers {
            developer {
                id.set("l2hyunwoo")
                name.set("l2hyunwoo")
                url.set("https://github.com/l2hyunwoo")
            }
        }
        scm {
            url.set("https://github.com/l2hyunwoo/apps-in-toss-cmp-sdk")
            connection.set("scm:git:https://github.com/l2hyunwoo/apps-in-toss-cmp-sdk.git")
            developerConnection.set("scm:git:ssh://git@github.com/l2hyunwoo/apps-in-toss-cmp-sdk.git")
        }
    }
}
