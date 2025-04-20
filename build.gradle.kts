plugins {
    id("java")
}

group = "me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2.jb"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2")

    implementation("commons-cli:commons-cli:1.9.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.mockito:mockito-core:5.17.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.17.0")
    testImplementation("org.apache.commons:commons-lang3:3.17.0")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2.Main"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks.test {
    useJUnitPlatform()
}
