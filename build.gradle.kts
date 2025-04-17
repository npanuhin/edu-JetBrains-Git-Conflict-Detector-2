plugins {
    id("java")
}

group = "me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2.jb"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
