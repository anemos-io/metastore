# Meta-Store

Warning this is an evolving product, even **Pre-Alpha** quality. Don't use
this in production just yet.

## Description

Utilities for handling and converting ProtocolBuffers. Focused but not
limited to Apache Beam.

### Features

- Recreate original .proto files for a binary schema.
    
### Why ProtoBuf

- ProtoBuf has a good DSL, with contract first in mind.
- ProtoBuf has a binary versioning strategy: A message can even be decoded
  without a schema.
- ProtoBuf has a collection of Well Known Types: Native timestamps.
- ProtoBuf Java implementation has a powerfull schema API.
- ProtoBuf has options so we can change the behaviour of the pipeline.
- ProtoBuf has good tooling for generating cross language code.

### Known Issues

- It's not implemented yet

### Rules

| name | validation | submit | description |  
|---|---|---|---|
| field_add | | | Registry has knowledge of field |
| field_remove | | | Registry has no knowledge of field |
| field_deprecate | | | Registry knowns field is deprecated | 

## Roadmap

- ProtoBuf to BigQuery DDL
- ProtoBuf to Spanner
- ProtoBuf to/from BSON
- ProtoBuf to/from Avro
- Recursive messages
- OneOf type
- Well Known Types
- Wrapper Types
- Plugable message type convertions
- Options support for
    - Description
    - Deprecation
    - Flattening
    - Event time
- Register standard Options
