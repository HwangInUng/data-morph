plugins {
    java
    `java-library`
    `maven-publish`
}

group = "io.github.datamorph"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    // JitPack을 위한 Sources JAR 생성
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "DataMorph Team",
            "Automatic-Module-Name" to "io.datamorph"
        )
    }
}

// Javadoc 생성 시 경고 무시 (한국어 주석 때문에)
tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

// JitPack을 위한 publishing 설정
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.datamorph"
            artifactId = "data-morph"
            version = project.version.toString()
            
            from(components["java"])

            pom {
                name.set("DataMorph")
                description.set("Lightweight Java library for flexible data transformation and processing")
                url.set("https://github.com/HwangInUng/data-morph")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("inung")
                        name.set("HwangInWoong")
                        email.set("ung6860@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/HwangInUng/data-morph.git")
                    developerConnection.set("scm:git:ssh://github.com/HwangInUng/data-morph.git")
                    url.set("https://github.com/HwangInUng/data-morph")
                }
            }
        }
    }
}
