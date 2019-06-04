package io.anemos.metastore.config;

public class RegistryConfig {

  public String name;
  public String shadowOf;
  public GitConfig git;

  public RegistryConfig() {}

  public RegistryConfig(String name) {
    this.name = name;
  }

  public RegistryConfig(String name, String shadowOf) {

    this.shadowOf = shadowOf;
    this.name = name;
  }
}
