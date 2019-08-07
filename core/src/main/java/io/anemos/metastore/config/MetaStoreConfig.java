package io.anemos.metastore.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

public class MetaStoreConfig {

  public StorageProviderConfig storage;

  public StorageProviderConfig bind;

  public RegistryConfig registries[];

  public GitGlobalConfig git;

  public MetaStoreConfig resolve() {
    final Logger logger = Logger.getLogger(MetaStoreConfig.class.getName());
    if (registries == null) {
      System.out.println("No repositories configured, creating default repo");
      registries = new RegistryConfig[] {new RegistryConfig("default")};
    }
    for (RegistryConfig registry : registries) {

      if(bind != null) {
        if(registry.bind == null) {
          registry.bind = new BindConfig();
        }
      }
      if(registry.bind.providers == null || registry.bind.providers.length == 0) {
        logger.info(String.format("Adding global bind provider to %s as primary.", registry.name));
        registry.bind.providers = new StorageProviderConfig[] { bind };
      }
      else {
        ArrayList<StorageProviderConfig> tmp = new ArrayList<>(Arrays.asList(registry.bind.providers));
        tmp.add(0, bind);
        registry.bind.providers = tmp.toArray(new StorageProviderConfig[0]);
      }

      if(storage != null) {
        if(registry.storage != null) {
          registry.storage = storage;
        }
      }
    }
    return this;
  }


}
