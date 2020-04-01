package io.anemos.metastore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaStoreConfig {
  private static final Logger LOG = LoggerFactory.getLogger(MetaStoreConfig.class);
  private boolean resolved = false;
  private ProviderConfig storage;
  private RegistryConfig registries[];
  private GitGlobalConfig git;

  public ProviderConfig getStorage() {
    return storage;
  }

  public void setStorage(ProviderConfig storage) {
    this.storage = storage;
  }

  public RegistryConfig[] getRegistries() {
    return registries;
  }

  public void setRegistries(RegistryConfig[] registries) {
    this.registries = registries;
  }

  GitGlobalConfig getGlobalGit() {
    return git;
  }

  public void setGit(GitGlobalConfig git) {
    this.git = git;
  }

  public MetaStoreConfig resolve() {
    if (resolved) {
      return this;
    }
    if (registries == null) {
      LOG.info("No repositories configured, creating default repo");
      registries = new RegistryConfig[] {new RegistryConfig("default")};
    }
    for (RegistryConfig registry : registries) {
      registry.resolve(storage, git);
    }
    return this;
  }
}
