package io.anemos.metastore.config;

public class GitRegistryConfig {
  public String path;
  public String remote;
  public String privateKey;

  public GitRegistryConfig() {}

  public GitRegistryConfig(String path) {
    this.path = path;
  }
}
