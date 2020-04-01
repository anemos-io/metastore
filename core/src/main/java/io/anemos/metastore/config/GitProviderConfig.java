package io.anemos.metastore.config;

public class GitProviderConfig {
  public String path;
  public String remote;
  public String privateKey;

  public GitProviderConfig() {}

  public GitProviderConfig(String path) {
    this.path = path;
  }
}
