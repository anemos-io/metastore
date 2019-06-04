package io.anemos.metastore.config;

public class StorageProviderConfig {

  public String providerClass;
  public Parameters[] parameters;

  public static class Parameters {
    public String name;
    public String value;

    public Parameters() {}

    public Parameters(String name, String value) {
      this.name = name;
      this.value = value;
    }
  }
}
