## Configuration

### Top level

```yaml
storage:
  providerClass: io.anemos.metastore.provider.LocalFileStorage
  parameters:
    - name: path
      value: /workspace/registries
registries:
  - name: default
    bind:
    - providerClass: io.anemos.metastore.provider.LocalFileStorage
      parameters:
        - name: path
          value: /workspace/registries
git:
  privateKey: LS0tLS1CR...LS0tCg==
  hosts:
    - host: "[source.developers.google.com]:2022"
      key: AGvEpqYNMqs...ZsBn434
      type: SHA256
```

- storage: global storage provider, if specified you don't need to specify a 
provider per registry
- registries: list of registries
- git: global git settings, if specified you don't need to repeat this for the
registries

### storage 

See [providers](providers.md) for a list of supported providers.

### registries

See [registry](config_registry.md) configuration for details of configuring registries.

### git

See [ssh](ssh.md) for details on how to abtain these parameters.
