import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "2.4.20"

}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}




kotlin {
    jvm()
    val hostOs = System.getProperty("os.name")
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries {
            sharedLib {
                baseName = when {
                    hostOs.startsWith("Windows") -> "libnative"
                    hostOs == "Linux" || hostOs == "Mac OS X" -> "native"
                    else -> throw GradleException("Unsupported host OS: $hostOs")
                }
            }
        }
    }
    when {
        hostOs == "Mac OS X" -> macosArm64("nativeMac")
        hostOs == "Linux" -> linuxX64("nativeLinux")
        hostOs.startsWith("Windows") -> mingwX64("nativeWindows")
        else -> throw GradleException("Unsupported host OS: $hostOs")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(kotlin("stdlib"))
        }
        nativeMain.dependencies {
            implementation(kotlin("stdlib"))
        }
        jvmMain.dependencies {
            implementation(kotlin("stdlib"))
        }
    }
}

tasks.wrapper {
    gradleVersion = "9.8.0"
    distributionType = Wrapper.DistributionType.ALL
}

