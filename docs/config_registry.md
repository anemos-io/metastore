## Registry Configuration

```yaml
registries:
  - name: default
    bind:
    - providerClass: io.anemos.metastore.provider.LocalFileStorage
      parameters:
        - name: path
          value: /workspace/registries
```
