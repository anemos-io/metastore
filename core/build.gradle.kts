import com.google.protobuf.gradle.*
import org.gradle.kotlin.dsl.provider.gradleKotlinDslOf

val repoUrl: String by project

plugins {
    id("net.ltgt.apt") version "0.20"
    id("com.google.protobuf") version "0.8.8"
    `java-library`
    `maven-publish`
    idea
}

repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    compile("com.google.protobuf:protobuf-java:3.6.1")
    compile("io.grpc:grpc-stub:1.17.1")
    compile("io.grpc:grpc-protobuf:1.17.1")
    runtime("io.grpc:grpc-netty-shaded:1.17.1")
    compile("io.grpc:grpc-services:1.17.1")
    compile("org.eclipse.jgit:org.eclipse.jgit:5.2.1.201812262042-r")

    implementation("com.google.code.gson:gson:2.8.5")

    testImplementation("junit:junit:4.12")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.6.1"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.15.1"
        }
    }

    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without options.
                id("grpc")
            }
        }
        ofSourceSet("test")
    }
}


publishing {
    repositories {
        maven {
            // change to point to your repo, e.g. http://my.org/repo
            url = uri(repoUrl)
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
        }
    }
}