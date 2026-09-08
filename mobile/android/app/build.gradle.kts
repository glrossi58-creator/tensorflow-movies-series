import java.util.Base64
import java.util.Properties
import java.net.URI

plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

val defines = (project.findProperty("dart-defines") as? String).orEmpty().split(',').filter { it.isNotBlank() }
    .map { String(Base64.getDecoder().decode(it), Charsets.UTF_8) }
    .associate { it.substringBefore('=') to it.substringAfter('=') }
val apiUri = URI(defines["API_BASE_URL"] ?: "http://10.0.2.2:8080")
val apiHost = apiUri.host ?: error("API_BASE_URL exige um host válido")
val localHost = apiHost == "localhost" || apiHost == "127.0.0.1" ||
    apiHost.startsWith("10.") || apiHost.startsWith("192.168.") ||
    Regex("172\\.(1[6-9]|2[0-9]|3[01])\\..+").matches(apiHost)
require(apiUri.scheme == "https" || (apiUri.scheme == "http" && localHost)) { "Use HTTPS em produção. HTTP é permitido apenas para um host privado local." }
val localHttp = apiUri.scheme == "http" && localHost
val networkResources = layout.buildDirectory.dir("generated/lumeNetworkRes")
val generateNetworkPolicy by tasks.registering {
    inputs.property("apiHost", apiHost)
    inputs.property("localHttp", localHttp)
    outputs.dir(networkResources)
    doLast {
        val xml = networkResources.get().file("xml/network_security_config.xml").asFile
        xml.parentFile.mkdirs()
        val exception = if (localHttp) "<domain-config cleartextTrafficPermitted=\"true\"><domain includeSubdomains=\"false\">$apiHost</domain></domain-config>" else ""
        xml.writeText("<?xml version=\"1.0\" encoding=\"utf-8\"?><network-security-config><base-config cleartextTrafficPermitted=\"false\"/>$exception</network-security-config>")
    }
}
val signingProperties = Properties()
val signingFile = rootProject.file("key.properties")
if (signingFile.exists()) signingFile.inputStream().use { signingProperties.load(it) }

android {
    namespace = "com.gilrossi.movie_recommendation"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion
    sourceSets.getByName("main").res.srcDir(networkResources)

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.gilrossi.movie_recommendation"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        // Uses the version code from pubspec.yaml. When using split APKs, 1000 * ABI_VERSION
        // is added automatically by Flutter. (https://developer.android.com/studio/build/configure-apk-splits#configure-APK-versions)
        // You can force using the value of versionCode by specifying the `-P force-version-code-ignoring-abi=true`
        // flag during build.
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        if (signingFile.exists()) create("release") {
            keyAlias = signingProperties.getProperty("keyAlias")
            keyPassword = signingProperties.getProperty("keyPassword")
            storeFile = file(signingProperties.getProperty("storeFile"))
            storePassword = signingProperties.getProperty("storePassword")
        }
    }
    buildTypes {
        release {
            // Local installation uses the development certificate until an official keystore is supplied.
            signingConfig = signingConfigs.getByName(if (signingFile.exists()) "release" else "debug")
        }
    }
}
tasks.named("preBuild") { dependsOn(generateNetworkPolicy) }

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
