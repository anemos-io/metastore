## Shadow Registry

To have a clear split between configuration (through *Options*) and contract ownership
the metastore has a feature called shadow registry. It makes it 
possible for other parties, aside the contract owners, too augment the contracts, 
examples of this could be:

- Annotate a field to indicate a BigQuery partition column or Cluster column.
- Add descriptions to fields.
- Annotate a field to indicate it's a sensitive field (eg. Credit Cart).
- Annotate a field referencing a message to flatten it into a row.

A shadow registry is basically a copy of another registry but with delta's applied.
Every time an update happens on the original repo, the delta is applied again. If
you update the shadow registry (example: add an option), the delta is updated.

This is the most basic configuration to make a shadow registry

```yaml
registries:
  - name: default
  - name: shadow
    shadowOf: default
```

