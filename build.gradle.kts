plugins {
    java
    id("com.github.johnrengelman.shadow") version "4.0.4"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    maven(url="https://repo.codemc.org/repository/maven-public/")
    maven(url="https://repo.codemc.org/repository/nms/")
}

dependencies {
    compileOnly("org.spigotmc:spigot:1.12.2-R0.1-SNAPSHOT")
    implementation("com.oop.orangeengine:file:4.8")
    implementation("com.oop.orangeengine:engine:4.8")
    implementation("com.oop.orangeengine:command:4.8")
    implementation("com.oop.orangeengine:message:4.8")
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        destinationDirectory.set(file("out"))
        archiveFileName.set("AutoReload.jar")

        relocate("com.oop.orangeengine", "com.oop.autoreload.engine")
    }
    withType<ProcessResources> {
        eachFile {
            if (this.name.contentEquals("plugin.yml"))
                filter { it.replace("{project.version}", version.toString()) }
        }
    }
}
