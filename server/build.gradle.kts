import com.google.protobuf.gradle.*
import org.gradle.kotlin.dsl.provider.gradleKotlinDslOf

plugins {
    id("net.ltgt.apt") version "0.20"
    id("com.google.protobuf") version "0.8.8"
    java
    idea
}

repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    compile(project(":core"))
    compile("com.google.protobuf:protobuf-java:3.6.1")
    compile("io.grpc:grpc-stub:1.17.1")
    compile("io.grpc:grpc-protobuf:1.17.1")
    runtime("io.grpc:grpc-netty-shaded:1.17.1")
    compile("io.grpc:grpc-services:1.17.1")
    compile("org.eclipse.jgit:org.eclipse.jgit:5.2.1.201812262042-r")

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
    }
}
