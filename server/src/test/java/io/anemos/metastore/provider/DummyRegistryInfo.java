package io.anemos.metastore.provider;

public class DummyRegistryInfo implements RegistryInfo {
  @Override
  public String getName() {
    return "test";
  }

  @Override
  public String getUri() {
    return null;
  }
}
