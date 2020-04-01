package io.anemos.metastore.config;

public class GitGlobalConfig {

  public static class GitHostConfig {
    public String host;
    public String key;
    public String type;
  }

  public GitHostConfig hosts[];
  public String privateKey;
}
