package io.anemos.metastore.config;

public class GitConfig {
  public String path;
  public String remote;

  public GitConfig() {}

  public GitConfig(String path) {
    this.path = path;
  }
}
