plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}
kotlin {
    js {
        browser()
        binaries.executable()
    }
    wasmJs {
        browser()
        binaries.executable()
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared-ui"))
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.coroutines.core)
            implementation("io.github.l2hyunwoo:apps-in-toss-sdk:0.1.0-SNAPSHOT")
        }
    }
}

val sdkWebResources = configurations.create("sdkWebResources")
dependencies {
    add(sdkWebResources.name, "io.github.l2hyunwoo:apps-in-toss-sdk:0.1.0-SNAPSHOT:web-resources@jar")
}
tasks.register<Sync>("prepareAppsInTossBridge") {
    dependsOn(sdkWebResources)
    from({ sdkWebResources.map { zipTree(it) } })
    into(layout.buildDirectory.dir("ait-cmp-sdk"))
}
