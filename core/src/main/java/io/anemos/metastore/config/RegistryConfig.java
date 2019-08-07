package io.anemos.metastore.config;

public class RegistryConfig {

  public String name;
  public String shadowOf;
  public GitRegistryConfig git;

  public StorageProviderConfig storage;
  public BindConfig bind;

  public String[] scope;

  public RegistryConfig() {}

  public RegistryConfig(String name) {
    this.name = name;
  }

  public RegistryConfig(String name, String shadowOf, String[] scope) {
    this.shadowOf = shadowOf;
    this.name = name;
    this.scope = scope;
  }
}
