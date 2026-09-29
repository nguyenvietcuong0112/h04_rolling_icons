@file:Suppress("DEPRECATION")

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val formattedDate = SimpleDateFormat("ddMMyyyy", Locale.US).format(Date())
android {
    namespace = "com.iconchanger.rollingicons"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.iconchanger.rollingicons"
        minSdk = 24
        targetSdk = 37
        versionCode = 100
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        dataBinding = true
    }
}

androidComponents {
    onVariants { variant ->
        val versionName = android.defaultConfig.versionName ?: "0.0.5"
        val versionCode = android.defaultConfig.versionCode ?: 5
        val type = variant.buildType ?: variant.name

        variant.outputs.forEach { output ->
            output.outputFileName.set(
                "D72_RollingIcons_v${versionName}_c${versionCode}_${formattedDate}-${type}.apk"
            )
        }
    }
}

tasks.matching { it.name == "bundleRelease" }.configureEach {
    doLast {
        val bundleDir = layout.buildDirectory.dir("outputs/bundle/release").get().asFile
        bundleDir.listFiles { f -> f.extension == "aab" }?.forEach { aab ->
            val newName =
                "D72_RollingIcons_v${android.defaultConfig.versionName}_c${android.defaultConfig.versionCode}_${formattedDate}-release.aab"
            aab.renameTo(File(aab.parent, newName))
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-Xuse-k2=false",
            "-Xskip-metadata-version-check"
        )
    }
}

val natives by configurations.creating

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("com.intuit.sdp:sdp-android:1.1.0")
    implementation("com.intuit.ssp:ssp-android:1.1.0")


    // libGDX
    val gdxVersion = "1.14.2"
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")

    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")

    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-x86_64")

    // Image loading (Coil)
    implementation("io.coil-kt:coil:2.5.0")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Lottie animations
    implementation("com.airbnb.android:lottie:6.4.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
    implementation("com.google.android.gms:play-services-ads")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-database")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")


    //lib ads
    implementation("com.app:library-ads:0.0.1")

    // sdk mediation
    implementation("com.facebook.android:facebook-android-sdk:18.3.0")
    implementation("com.google.ads.mediation:facebook:6.22.0.0")
    implementation("com.google.ads.mediation:applovin:13.6.4.0")
    implementation("com.google.ads.mediation:inmobi:11.4.0.0")
    implementation("com.google.ads.mediation:pangle:8.2.0.4.0")
    implementation("com.google.ads.mediation:mintegral:17.1.71.0")
    implementation("com.unity3d.ads:unity-ads:4.20.0")
    implementation("com.google.ads.mediation:unity:4.19.0.1")
    implementation("com.pubscale.ads:admob-adapter:1.0.5")
}

tasks.register("copyAndroidNatives") {
    doFirst {
        val jniDir = file("src/main/jniLibs")
        val abis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        abis.forEach { File(jniDir, it).mkdirs() }

        configurations.getByName("natives").files.forEach { jar ->
            val abi = when {
                jar.name.contains("natives-arm64-v8a") -> "arm64-v8a"
                jar.name.contains("natives-armeabi-v7a") -> "armeabi-v7a"
                jar.name.contains("natives-x86_64") -> "x86_64"
                jar.name.contains("natives-x86") -> "x86"
                else -> null
            }
            if (abi != null) {
                copy {
                    from(zipTree(jar))
                    into(File(jniDir, abi))
                    include("*.so")
                }
            }
        }
    }
}

tasks.matching { it.name.contains("merge") && it.name.contains("JniLibFolders") }.configureEach {
    dependsOn("copyAndroidNatives")
}


