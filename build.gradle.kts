plugins {
    java
    `java-library`
    `maven-publish`
}

group = "com.datamorph"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    // Javadoc과 Sources JAR 생성
    withJavadocJar()
    withSourcesJar()
}

dependencies {

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.assertj:assertj-core:3.24.2")

    // Benchmark
    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
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
            "Automatic-Module-Name" to "com.datamorph"
        )
    }
}

// Maven 배포 설정 (나중에 사용)
//publishing {
//    publications {
//        create<MavenPublication>("maven") {
//            from(components["java"])
//
//            pom {
//                name.set("DataMorph")
//                description.set("Lightweight Java library for flexible data transformation")
//                url.set("https://github.com/yourusername/datamorph")
//
//                licenses {
//                    license {
//                        name.set("The Apache License, Version 2.0")
//                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
//                    }
//                }
//
//                developers {
//                    developer {
//                        id.set("yourid")
//                        name.set("Your Name")
//                        email.set("your.email@example.com")
//                    }
//                }
//            }
//        }
//    }
//}