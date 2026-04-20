plugins {
    `kotlin-dsl`
    `java-gradle-plugin`

    alias(libs.plugins.commons)
    alias(libs.plugins.publish)
    alias(libs.plugins.dotenv)
}

group = project.property("group") as String
version = project.property("version") as String

repositories {
    mavenCentral()
    maven("https://maven.lyranie.dev")
}

dependencies {
    implementation(libs.berry.parser)
}

gradlePlugin {
    plugins {
        create("berry-gradle") {
            id = "dev.lyranie.berry-gradle"
            implementationClass = "dev.lyranie.berry.BerryGradlePlugin"
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            artifactId = project.property("artifact") as String
        }
    }

    repositories {
        maven {
            url = uri("https://repo.repsy.io/lyranie/maven")
            credentials {
                username = env.fetch("REPSY_USERNAME", "")
                password = env.fetch("REPSY_PASSWORD", "")
            }
        }
    }
}

tasks {
    publish {
        dependsOn(build)
    }
}
