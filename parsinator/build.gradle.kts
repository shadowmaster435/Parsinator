import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget



plugins {
    kotlin("multiplatform") version "2.4.20"
}

group = "net.shadowmaster435"
version = "1.0"

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
                    hostOs.startsWith("Windows") -> "parsinator-windows"
                    hostOs == "Linux" || hostOs == "Mac OS X" -> "parsinator-unix"
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



tasks.register<Zip>("packageLinuxKlib") {
    archiveFileName.set("parsinator-linux-${version}.klib")

    destinationDirectory.set(layout.buildDirectory.dir("libs"))

    // Define what files to include in the ZIP
    from(layout.buildDirectory.dir("classes/kotlin/nativeLinux/main/klib/parsinator"))
}

tasks.findByPath(":parsinator:assemble")?.finalizedBy("packageLinuxKlib")
tasks.findByPath(":wrapper")?.let { wrapper ->
    wrapper as Wrapper
    wrapper.gradleVersion = "9.8.0"
    wrapper.distributionType = Wrapper.DistributionType.ALL
}