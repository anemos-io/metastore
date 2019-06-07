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

Start by creating a local test directory called `work`. If you
name the directory work it's safe for accidental updates as it's ignored in the 
`.gitignore` file. So start by running: 

`mkdir work`

Let's create two remote ssh git repos (see [ssh.md]) called `metastore-test-default`
and `metastore-test-shadow`. You can use your favorite git repository service as
long as it support SSH access.

Now create the `work/config.yaml` file that will be the configuration file of
metastore.

```yaml
storage:
  providerClass: io.anemos.metastore.provider.LocalFileProvider
  parameters:
    - name: path
      value: /path_to/work/test/registries
registries:
  - name: default
    git:
      remote: ssh://user@example.com@source.developers.google.com:2022/p/example-project/r/metastore-test-default
      path: /path_to/work/test/git/default
  - name: shadow
    shadowOf: default
    git:
      remote: ssh://user@example.com@source.developers.google.com:2022/p/example-project/r/metastore-test-shadow
      path: /path_to/work/test/git/shadow
git:
  privateKey: LS0tLS1CRUdJT....EtFWS0tLS0tCg==
  hosts: qwerty
```

### Server Startup

**Note: The Docker container is not ready. This will be fixed in a later version.
Now, you'll need to run it in your favorite IDE with gradle support!**

Once you have your project imported into your favorite IDE you can start the server
by executing 
[MetaStoreServer.java](../server/src/main/java/io/anemos/metastore/MetaStoreServer.java).
But before you start, make sure to set the path. At this stage you need todo this
by setting an environment variable:

`METASTORE_CONFIG_PATH=/path_to/metastore/work/config.yaml`

One the server is started you can start interacting with the API. The server support
Server Reflection, so it's ideal to use a tool that supports it. My favorite one is
[grpcui](https://github.com/fullstorydev/grpcui). Download it and start it with:

`grpcui -plaintext localhost:8980`

Now it's time to play.

### Simple registry update

First of all it's important to know that metastore does not work with the textual
representation of protobuf, but with it's binary counterpart 
(see [Architecture](architecture.md)) called a FileDescriptorSet. Getting this 
form is done through compiling them to the binary form through `protoc` or other
means (later will introduce `metastep`. For now we have pre-compiled binary examples
in this repo. You can find them under
`server/src/test/resources/io/anemos/metastore/server/` compiled and the sources
in the `testsets` folder.

Now let's submit on of these examples. As grpcui is a web tool, we'll need to 
base64 it to be able to submit it into the web ui (note, if you work with the
api's in your produce this is not necessary).

Get the base64 version of the base contracts:
`base64 server/src/test/resources/io/anemos/metastore/server/base.pb`

Now submit them through grpcui to the **default** repo. Fill in:

**SchemaRegistryService->SubmitSchema()**

* registry_name : **default**
* fd_proto_set: `base64 representation`

Leave `scope` as is and invoke. Now have a look at both configurated git
repositories. They should contain the textual version of the contracts.

Now let's enhance the contracts with some options we'll be using in the next
exercise. Do the same as above, again to the **default** registry:

`base64 server/src/test/resources/io/anemos/metastore/server/base_known_option.pb`

and submit:

**SchemaRegistryService->SubmitSchema()**

* registry_name : **default**
* fd_proto_set: `base64 representation`

After verifying the git repositories it's time to play with the shadow registry.

### Shadow update

Now, say you are in another team and want to enhance the contract with some metadata
but are not allowed to tough the original contracts. No worries, we've configured
a shadow registry.

Let's add a field option and submit it to the shadow repository.

`base64 server/src/test/resources/io/anemos/metastore/server/base_add_field_option.pb`

**SchemaRegistryService->SubmitSchema()**

* registry_name : **shadow**
* fd_proto_set: `base64 representation`

Now have a look at both git repositories, they should now be deverged. The shadow
one should have a field with a field option.

Now let's see what happens when you add something to the default repository.

`base64 server/src/test/resources/io/anemos/metastore/server/shadow_default_field_added.pb`

**SchemaRegistryService->SubmitSchema()**

* registry_name : **default**
* fd_proto_set: `base64 representation`

Go and look at both the repositories, you will see that the shadow repo also has
the extra field applied, and the field option unique to the shadow also keeps on
being applied.

This concludes the first training, more tutorials to come...