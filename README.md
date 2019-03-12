# Protobuf MetaStore

Warning this is an evolving product, even **Pre-Alpha** quality. Don't use
this in production just yet.

## Description

A schema registry for Protobuf and gRPC. It will guard against contract braking
and also makes sure the proto contracts are linted against the best practises.

It main focus is to be run as a service where gRPC microservices and data pipelines
should validate it's producing/serving and consuming contracts. This way SRE's 
can keep track of the contracts that are in use throughout the organisation.

It can alse be used as a build step in a container based CI system.

### Features


### Known Issues

- Uses some hard dependencies on protobeam.
- It's not implemented yet.

### Rules



## Roadmap

## Quick setup

Create a file `gradle/properties.gradle`

```
ext {
    snapshotRepoUrl = "https ://you.maven.repo.example/repository/maven-snapshots/"
    repoUrl = "https ://you.maven.repo.example/repository/maven-releases/"
    repoUser = "repo_user"
    repoPass = "supersecret"

    containerRepoBase = "eu.gcr.io/my-repo"
    conrainerRepoTarget = "eu.gcr.io/my-repo"
}
```

Build the base images that jib uses, see README.md in base directory.

Build protobeam (metastore has a dependency, but is not published to mavenrepo) and
publish to local maven repo.