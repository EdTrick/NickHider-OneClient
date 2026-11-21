plugins {
    java
    id("net.fabricmc.fabric-loom-remap") version "1.17-SNAPSHOT"
    id("ploceus") version "1.17-SNAPSHOT"
}

version = "1.1.1"
group = "dev.edtrick"

base {
    archivesName.set("nickhider")
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net")
    maven("https://maven.ornithemc.net/releases")
    maven("https://maven.ornithemc.net/snapshots")
    maven("https://repo.polyfrost.org/releases")
    maven("https://repo.polyfrost.org/snapshots")
}

ploceus {
    setIntermediaryGeneration(2)
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings(ploceus.mcpMappings("stable", "1.8.9", "22"))
    modImplementation("net.fabricmc:fabric-loader:0.20.1")
    compileOnly(fileTree("libs") { include("*.jar") })
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(8)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
