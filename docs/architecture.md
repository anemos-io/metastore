## Architecture

Metastore heavily relies on the binary form of protobuf itself. One of protobuf's
best kept secret is the Descriptor, the binary form of the Protobuf contract. The
Java implementation of Protobuf has a very rich implementation for manipulating the
Descriptors.

TODO explain more