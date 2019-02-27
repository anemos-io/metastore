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

Add the following to the `gradle.properties`

```snapshotRepoUrl=https://you.maven.repo.example.com/repository/maven-snapshots/
repoUrl=https://you.maven.repo.example.com/repository/maven-releases/
repoUser=repo_user
repoPass=repo_password```

