

This tutorial walks you tru the simplest, most bare-bones configuration of 
the Metastore.





Prepare local test directory

`mkdir work`


Create two remote ssh git repos (see [ssh.md]). 

Create `work/config.yaml`

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

## Server Startup


Start grpcui

`grpcui -plaintext localhost:8980`


## Simple registry update

Submit initial set of contracts

default
`base64 server/src/test/resources/io/anemos/metastore/server/base.pb`

SchemaRegistryService->SubmitSchema()

registry_name : default
fd_proto_set: <base64>


default
`base64 server/src/test/resources/io/anemos/metastore/server/base_known_option.pb`

## Shadow update

shadow
`base64 server/src/test/resources/io/anemos/metastore/server/base_add_field_option.pb`


default
`base64 server/src/test/resources/io/anemos/metastore/server/shadow_default_field_added.pb`