package io.anemos.metastore.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaStoreConfig {
  private static final Logger LOG = LoggerFactory.getLogger(MetaStoreConfig.class);
  private boolean resolved = false;
  private ProviderConfig storage;
  private RegistryConfig registries[];
  private Map<String, RegistryConfig> registryMap;
  private GitGlobalConfig git;

  public ProviderConfig getStorage() {
    return storage;
  }

  public void setStorage(ProviderConfig storage) {
    this.storage = storage;
  }

  public Collection<RegistryConfig> getRegistryConfigs() {
    if (!resolved) {
      throw new IllegalStateException();
    }
    return registryMap.values();
  }

  public RegistryConfig getRegistryConfig(String name) {
    if (!resolved) {
      throw new IllegalStateException();
    }
    if (!registryMap.containsKey(name)) {
      throw new IllegalArgumentException("RegistryConfig not found");
    }
    return registryMap.get(name);
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
    registryMap = new HashMap<>();
    for (RegistryConfig registry : registries) {
      registryMap.put(registry.getName(), registry);
      registry.resolve(storage, git);
    }
    resolved = true;
    return this;
  }
}
