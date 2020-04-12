package io.anemos.metastore.core.registry;

import io.anemos.metastore.config.MetaStoreConfig;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.v1alpha1.RegistryP.Note;
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

    for (RegistryConfig registryConfig : this.config.getRegistryConfigs()) {
      AbstractRegistry intance;
      if (registryConfig.getShadowOf() != null) {
        intance = new ShadowRegistry(this, registryConfig);
        shadows.put(registryConfig.getName(), intance);
      } else {
        intance = new SchemaRegistry(this, registryConfig);
        defaults.put(registryConfig.getName(), intance);
      }
      registries.put(registryConfig.getName(), intance);
    }

    defaults.values().forEach(registry -> registry.init());
    shadows.values().forEach(registry -> registry.init());

    shadows
        .values()
        .forEach(
            registry -> {
              String name = registry.registryConfig.getShadowOf();
              if (!shadowSubscribers.containsKey(name)) {
                shadowSubscribers.put(name, new ArrayList<>());
              }
              shadowSubscribers.get(name).add(registry);
            });
  }

  void notifyShadows(String name, Note note) {
    List<AbstractRegistry> registries = shadowSubscribers.get(name);
    if (registries != null) {
      registries.forEach(registry -> registry.update(note));
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
