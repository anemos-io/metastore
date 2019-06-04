package io.anemos.metastore;

import io.anemos.metastore.config.ConfigLoader;
import io.anemos.metastore.config.MetaStoreConfig;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.config.StorageProviderConfig;
import io.anemos.metastore.core.registry.Registries;
import io.anemos.metastore.provider.StorageProvider;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class MetaStore {
  private static final Logger logger = Logger.getLogger(MetaStore.class.getName());
  Registries registries;
  // public ProtoDescriptor repo;
  // public AbstractRegistry.ShadowRegistry shadowRegistry;
  private StorageProvider provider;

  public MetaStore() throws IOException {
    String configPath = System.getenv("METASTORE_CONFIG_PATH");
    MetaStoreConfig config;
    if (configPath == null) {
      System.out.println("Default config not set, running in demo mode");
      config = new MetaStoreConfig();
    } else {
      config = ConfigLoader.load(configPath);
    }
    init(config);
  }

  public MetaStore(MetaStoreConfig config) throws IOException {
    init(config);
  }

  /** Create a RouteGuide server listening on {@code port} using {@code featureFile} database. */
  public void init(MetaStoreConfig config) throws IOException {

    if (config.storage == null) {
      System.out.println("Storage Provider not configured, defaulting to in memory provider");
      config.storage = new StorageProviderConfig();
      config.storage.providerClass = "io.anemos.metastore.provider.InMemoryProvider";
    }

    try {
      Map<String, String> parameters = new HashMap<>();
      for (StorageProviderConfig.Parameters parameter : config.storage.parameters) {
        parameters.put(parameter.name, parameter.value);
      }

      provider =
          (StorageProvider)
              Class.forName(config.storage.providerClass)
                  .getConstructor(Map.class)
                  .newInstance(parameters);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (config.registries == null) {
      System.out.println("No repositories configured, creating default repo");
      config.registries = new RegistryConfig[] {new RegistryConfig("default")};
    }

    registries = new Registries(config, provider);

    // repo = new ProtoDescriptor(provider.read("default.pb").toByteArray());
    // Report shadowDelta = Report.parseFrom(provider.read("shadow.pb"));

    // shadowRegistry = new ShadowRegistry(this, registries, con);
  }
}
