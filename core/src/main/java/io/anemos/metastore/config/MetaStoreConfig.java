package io.anemos.metastore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaStoreConfig {
  private static final Logger LOG = LoggerFactory.getLogger(MetaStoreConfig.class);

  public ProviderConfig storage;

  public RegistryConfig registries[];

  public GitGlobalConfig git;

  public MetaStoreConfig resolve() {
    if (registries == null) {
      LOG.info("No repositories configured, creating default repo");
      registries = new RegistryConfig[] {new RegistryConfig("default")};
    }
    for (RegistryConfig registry : registries) {
      //      if (registry.bind == null) {
      //        registry.bind = new ProvidersConfig();
      //      }

      if (storage != null) {
        if (registry.storage != null) {
          registry.storage = storage;
        }
      }
    }
    return this;
  }
}
