package io.anemos.metastore;

import io.anemos.metastore.config.ConfigLoader;
import io.anemos.metastore.config.MetaStoreConfig;
import io.anemos.metastore.core.registry.Registries;

public class MetaStore {
  Registries registries;

  public MetaStore(String configPath) {
    MetaStoreConfig config;
    if (configPath == null) {
      System.out.println("Default config not set, running in demo mode");
      config = new MetaStoreConfig();
    } else {
      config = ConfigLoader.load(configPath);
    }
    init(config.resolve());
  }

  public MetaStore(MetaStoreConfig config) {
    init(config);
  }

  /** Create a RouteGuide server listening on {@code port} using {@code featureFile} database. */
  public void init(MetaStoreConfig config) {
    registries = new Registries(config);
  }
}
