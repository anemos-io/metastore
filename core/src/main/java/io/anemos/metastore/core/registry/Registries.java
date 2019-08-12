package io.anemos.metastore.core.registry;

import io.anemos.metastore.config.MetaStoreConfig;
import io.anemos.metastore.config.RegistryConfig;
import io.grpc.Status;
import io.grpc.StatusException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Registries {

  private final MetaStoreConfig config;
  private Map<String, AbstractRegistry> registries = new HashMap<>();
  private Map<String, List<AbstractRegistry>> shadowSubscribers = new HashMap<>();

  private Map<String, AbstractRegistry> shadows = new HashMap<>();
  private Map<String, AbstractRegistry> defaults = new HashMap<>();

  public Registries(MetaStoreConfig config) {
    this.config = config;

    for (RegistryConfig registry : this.config.registries) {
      AbstractRegistry intance;
      if (registry.shadowOf != null) {
        intance = new ShadowRegistry(this, config, registry, config.git);
        shadows.put(registry.name, intance);
      } else {
        intance = new SchemaRegistry(this, config, registry, config.git);
        defaults.put(registry.name, intance);
      }
      registries.put(registry.name, intance);
    }

    defaults.values().forEach(registry -> registry.init());
    shadows.values().forEach(registry -> registry.init());

    shadows
        .values()
        .forEach(
            registry -> {
              String name = registry.registryConfig.shadowOf;
              if (!shadowSubscribers.containsKey(name)) {
                shadowSubscribers.put(name, new ArrayList<>());
              }
              shadowSubscribers.get(name).add(registry);
            });
  }

  void notifyShadows(String name) {
    List<AbstractRegistry> registries = shadowSubscribers.get(name);
    if (registries != null) {
      registries.forEach(registry -> registry.update());
    }
  }

  public AbstractRegistry get(String name) throws StatusException {
    if (name.equals("")) {
      name = "default";
    }
    AbstractRegistry registry = registries.get(name);
    if (registry == null) {
      throw Status.NOT_FOUND
          .withDescription(String.format("Registry with name '%s' was not found."))
          .asException();
    }
    return registry;
  }
}
