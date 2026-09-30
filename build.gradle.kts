
plugins {
    kotlin("jvm") version "2.4.20"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))
        }
    }
}
