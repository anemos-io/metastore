package io.anemos.metastore;

import io.anemos.metastore.config.ConfigLoader;
import io.anemos.metastore.config.MetaStoreConfig;
import io.anemos.metastore.core.registry.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaStore {
  private static final Logger LOG = LoggerFactory.getLogger(MetaStore.class);
  Registries registries;

  public MetaStore(String configPath) {
    MetaStoreConfig config;
    if (configPath == null) {
      LOG.warn("Default config not set, running in demo mode");
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
