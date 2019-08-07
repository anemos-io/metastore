package io.anemos.metastore.config;

import java.util.logging.Logger;

public class MetaStoreConfig {

  public StorageProviderConfig storage;

  public RegistryConfig registries[];

  public GitGlobalConfig git;

  public MetaStoreConfig resolve() {
    final Logger logger = Logger.getLogger(MetaStoreConfig.class.getName());
    if (registries == null) {
      System.out.println("No repositories configured, creating default repo");
      registries = new RegistryConfig[] {new RegistryConfig("default")};
    }
    for (RegistryConfig registry : registries) {
      if (registry.bind == null) {
        registry.bind = new BindConfig();
      }

      if (storage != null) {
        if (registry.storage != null) {
          registry.storage = storage;
        }
      }
    }
    return this;
  }
}
