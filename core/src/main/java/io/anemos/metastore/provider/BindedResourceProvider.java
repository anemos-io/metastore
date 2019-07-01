package io.anemos.metastore.provider;

public interface BindedResourceProvider {

  void init(String schemaUrn);

  void assignSchemaToResource(String resourceUrn, String fileName, String messageName);

  BindingResult getSchemaForResource(String resourceUrn);
}
