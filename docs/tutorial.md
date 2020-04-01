## Tutorial

This tutorial walks you tru the simplest, most bare-bones configuration of 
the Metastore. Once configured we're only going to show the happy path of schema 
submission and how it interacts with the git repo, including the shadow registry.
So the goals are:

* Configuration introduction
* Submission of schema
* Interaction with git
* Introducing the shadow registry

### Preparation

Create a private docker network to play around in.

```bash
docker network create meta
```


Start by creating a local test directory called `work`. If you
name the directory work it's safe for accidental updates as it's ignored in the 
`.gitignore` file. So start by running: 

`mkdir work`

Let's create two remote ssh git repos (see [ssh.md]) called `metastore-test-default`
and `metastore-test-shadow`. You can use your favorite git repository service as
long as it support SSH access.

Now create the `work/tutorial/config.yaml` file that will be the configuration file of
metastore.

```yaml
storage:
  providerClass: io.anemos.metastore.provider.LocalFileStorage
  parameters:
    - name: path
      value: /workspace/registries
registries:
  - name: default
    git:
      remote: ssh://user@example.com@source.developers.google.com:2022/p/example-project/r/metastore-test-default
      path: /workspace/git/default
  - name: shadow
    shadowOf: default
    scope: ["test"]
    git:
      remote: ssh://user@example.com@source.developers.google.com:2022/p/example-project/r/metastore-test-shadow
      path: /workspace/git/shadow
git:
  privateKey: LS0tLS1CR...LS0tCg==
  hosts:
    - host: "[source.developers.google.com]:2022"
      key: AGvEpqYNMqs...ZsBn434
      type: SHA256
```

### Server Startup


#### Start Metastore using docker

Metastore is available on docker hub, and can be started as:

```bash
docker run -v `pwd`/work/tutorial:/workspace \
    --net meta --name metastore \
    metastore/metastore \
    --config /workspace/config.yaml
```


#### Start Metastore in your IDE 

If you want todo local development, start it in your favorite IDE. Once you have 
your project imported into your favorite IDE you can start the server by executing 
[MetaStoreServer.java](../server/src/main/java/io/anemos/metastore/MetaStoreServer.java).
But before you start, make sure to set the path. At this stage you need todo this
by setting an environment variable:

`METASTORE_CONFIG_PATH=/path_to/metastore/work/config.yaml`

One the server is started you can start interacting with the API. The server support
Server Reflection, so it's ideal to use a tool to explore the API. My favorite one is
[grpcui](https://github.com/fullstorydev/grpcui). Download it and start it with:

`grpcui -plaintext localhost:8980`

But let's follow the tutorial with metastep.

### Simple registry update

First of all it's important to know that metastore does not work with the textual
representation of protobuf, but with it's binary counterpart 
(see [Architecture](architecture.md)) called a FileDescriptor. If you use `protoc`
with the `descriptor_set_out` option you get a FileDescriptorSet, that is a collection
of FileDescriptors that the gRPC API accepts. But we will be using the `metastep` tool
to submit and verify our schemas. The testsets that we are using can be found under the
`testsets` folder.

Now let's submit the base set to the **default** repo. Execute the `metastep` through
the following docker command:

```bash
docker run -v `pwd`/testsets/base:/workspace \
    --net meta \
    metastore/metastep \
    publish \
    --workspace /workspace \
    --server metastore:8980 \
    --package_prefix test
```

The `metadata` step will compile all the proto files in the testset directory with
`protoc` and submit the resulting files to the store via the following API call:

*Registry->SubmitSchema()*

* registry_name : **default**
* repeated file_descriptor_proto: `base64 representation of file desciptors`

Metastore will verify and store the files, lets have a look at both configured git
repositories. They should contain the textual version of the contracts.

Now let's enhance the contracts with some options we'll be using in the next
exercise. Do the same as above, again to the **default** registry:

```bash
docker run -v `pwd`/testsets/base_known_option:/workspace \
    --net meta \
    metastore/metastep \
    publish \
    --workspace /workspace \
    --server metastore:8980 \
    --package_prefix test
```

Verify that the update has been submitted to the repository. Now with all the proto
files ready it's time to play with the shadow registry.

### Shadow update

Now, say you are in another team and want to enhance the contract with some metadata
but are not allowed to touch the original contracts. No worries, we've configured
a shadow registry.

Let's add a field option and submit it to the shadow repository.


```bash
docker run -v `pwd`/testsets/base_add_field_option:/workspace \
    --net meta \
    metastore/metastep \
    publish \
    --workspace /workspace \
    --server metastore:8980 \
    --registry shadow \
    --package_prefix test
```


docker run -v `pwd`/testsets/base_add_field_option:/var/workspace \
    ${METASTORE_DOCKER_REPO}/metastep \
    publish \
    --workspace /var/workspace \
    --server host.docker.internal:8980 \
    --registry shadow \
    --package_prefix test


This will submit the `base_add_field_option` test set, and submit it via the API
call:

*Registry->SubmitSchema()*

* registry_name : **shadow**
* repeated file_descriptor_proto: `base64 representation of file desciptors`

Now have a look at both git repositories, they should now be diverged. The shadow
one should have a field with a **extra** field option, that was defined in the
test set.

Now let's see what happens when you add something to the **default** repository.

```bash
docker run -v `pwd`/testsets/shadow_default_field_added:/workspace \
    --net meta \
    metastore/metastep \
    publish \
    --workspace /workspace \
    --server metastore:8980 \
    --package_prefix test
```

Go and look at both the repositories, you will see that the shadow repo also has
the extra field applied, and the field option unique to the shadow also keeps on
being applied.

This concludes the first training, lets clean up. More tutorials to come...

```bash
docker container rm metastore
docker container rm metastore
docker network rm meta
```
