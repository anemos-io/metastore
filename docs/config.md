## Configuration

Metastore server startup class is: `io.anemos.metastore.MetaStoreServer` and provides
one program argument `--config`. This provides the path to the configuration file that is explained in this doc.

If no config parameter is given, the server will look for the `METASTORE_CONFIG_PATH`
environment variable, this can be used in a dockerized version of the metastore.

Example usage:

```
--config=/workspace/config.yaml
```

The configuation file has the following top-level configuration:

### Top level

- storage: global storage provider, if specified you don't need to specify a 
provider per registry
- registries: list of registries
- git: global git settings, if specified you don't need to repeat this for the
registries

Example:

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

### storage 

See [providers](providers.md) for a list of supported providers.

### registries

See [registry](config_registry.md) configuration for details of configuring registries.

### git

See [ssh](ssh.md) for details on how to abtain these parameters.
