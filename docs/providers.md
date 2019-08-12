## Providers

The metastore provides 3 classes of providers, each with there own purpose:

* Storage Provider
* Bind Provider
* Evening Provider: Provider the sends 

Metastore has the following build-in providers:

* Local File Storage
* Google Cloud Storage
* Google Pubsub
* In Memory

### Provider Classes

#### Storage Provider

Provider that abstracts away a storage system for storing descriptors.

#### Bind Provider

Provider for storing binding connections between a resource
and a message/service.

#### Evening Provider

Provider the sends notifications to other system about changes in contracts

### Configuring a provider

All provider have the same way of configuration:

```yaml
providerClass: providerClass
parameters:
  - name: key1
    value: value1
  - name: key2
    value: value2
```

### Build-in Providers

#### Local File Storage

Implements the following classes:
* Storage
* Bind

Provider Class: `io.anemos.metastore.provider.LocalFileStorage`

| key | description | example |
| --- |-------------| --------|
| path | Path on the filesystem where the files will be stored | /workspace/registries |

#### Google Cloud Storage

Implements the following classes:
* Storage
* Bind

Provider Class: `io.anemos.metastore.provider.GoogleCloudStorage`

| key | description | example |
| --- |-------------| --------|
| project | Google cloud project where the storage bucket lives | my-cloud-project |
| bucket  | Name of the bucket | my-bucket |
| path    | Path within the bucket | path/in/bucket |

#### Google Pubsub

Implements the following classes:
* Eventing

| key | description | example |
| --- |-------------| --------|
| project | Google where the pubsub topics live | my-cloud-project |
| topic_descriptor_change  | Pubsub topic that will publish the descriptor changes | metastore-descriptor-change |
| topic_binding_change | Pubsub topic that will publish the binding changes | metastore-binding-change |

Provider Class: `io.anemos.metastore.provider.GooglePubsub`

| key | description | example |
| --- |-------------| --------|
| project | Google cloud project where the storage bucket lives | my-cloud-project |
| bucket  | Name of the bucket | my-bucket |
| path    | Path within the bucket | path/in/bucket |

#### In Memory

Implements the following classes:
* Storage
* Bind

Provider Class: `io.anemos.metastore.provider.InMemoryStorage`

No parameters are required

### Creating your own provider

TODO