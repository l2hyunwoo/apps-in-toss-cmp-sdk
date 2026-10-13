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
            implementation(project(":sample:shared-ui"))
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.coroutines.core)
            implementation(project(":sdk"))
        }
    }
}

val sdkWebResources = configurations.create("sdkWebResources")
dependencies {
    add(sdkWebResources.name, project(mapOf("path" to ":sdk", "configuration" to "webBridgeResources")))
}
tasks.register<Sync>("prepareAppsInTossBridge") {
    dependsOn(sdkWebResources)
    from({ sdkWebResources.map { zipTree(it) } })
    into(layout.buildDirectory.dir("ait-cmp-sdk"))
}
