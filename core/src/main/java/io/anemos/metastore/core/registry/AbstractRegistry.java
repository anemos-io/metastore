package io.anemos.metastore.core.registry;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import io.anemos.metastore.config.GitGlobalConfig;
import io.anemos.metastore.config.MetaStoreConfig;
import io.anemos.metastore.config.ProviderConfig;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.core.proto.PContainer;
import io.anemos.metastore.provider.BindProvider;
import io.anemos.metastore.provider.BindResult;
import io.anemos.metastore.provider.EventingProvider;
import io.anemos.metastore.provider.RegistryInfo;
import io.anemos.metastore.provider.StorageProvider;
import io.anemos.metastore.v1alpha1.Registry;
import io.anemos.metastore.v1alpha1.Report;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public abstract class AbstractRegistry implements RegistryInfo {

  private static final Logger LOG = Logger.getLogger(AbstractRegistry.class.getName());
  protected final Registries registries;
  protected final String name;
  private List<BindProvider> bindProviders;
  protected List<EventingProvider> eventingProviders;
  MetaStoreConfig config;
  RegistryConfig registryConfig;
  final StorageProvider storageProvider;
  PContainer protoContainer;
  private MetaGit metaGit;

  AbstractRegistry(
      Registries registries,
      MetaStoreConfig config,
      RegistryConfig registryConfig,
      GitGlobalConfig global) {
    this.registries = registries;
    this.name = registryConfig.name;
    this.config = config;
    this.registryConfig = registryConfig;
    this.metaGit = new MetaGit(registryConfig, global);
    this.bindProviders = new ArrayList<>();
    this.eventingProviders = new ArrayList<>();

    if (config.storage == null) {
      System.out.println("Storage Provider not configured, defaulting to in memory provider");
      config.storage = new ProviderConfig();
      config.storage.providerClass = "io.anemos.metastore.provider.InMemoryStorage";
    }
    if (config.storage.parameters == null) {
      config.storage.parameters = new ProviderConfig.Parameters[] {};
    }

    storageProvider = loadProvider(StorageProvider.class, config.storage.providerClass);
    storageProvider.initForStorage(this, config.storage.getParameter());

    if (registryConfig.bind != null) {
      boolean writeOnly = false;
      for (ProviderConfig provider : registryConfig.bind) {
        BindProvider bindProvider = loadProvider(BindProvider.class, provider.providerClass);
        bindProvider.initForBind(this, provider.getParameter(), writeOnly);
        writeOnly = true;
        bindProviders.add(bindProvider);
      }
    }
    if (registryConfig.eventing != null) {
      for (ProviderConfig provider : registryConfig.eventing) {
        EventingProvider eventingProvider =
            loadProvider(EventingProvider.class, provider.providerClass);
        eventingProvider.initForChangeEvent(this, provider.getParameter());
        eventingProviders.add(eventingProvider);
      }
    }
  }

  public abstract void init();

  public abstract void update();

  public abstract ByteString raw();

  public abstract PContainer get();

  public abstract PContainer ref();

  public abstract void update(PContainer ref, PContainer in, Report report);

  void syncGitRepo(String message) {
    metaGit.sync(protoContainer, message);
  }

  void initGitRepo() {
    metaGit.init();
  }

  public void createResourceBinding(String linkedResource, String messageName) {
    Descriptors.Descriptor descriptor = protoContainer.getDescriptorByName(messageName);
    Descriptors.FileDescriptor fileDescriptor = descriptor.getFile();
    this.bindProviders.forEach(
        provider -> provider.createResourceBinding(linkedResource, descriptor));
  }

  public void updateResourceBinding(String linkedResource, String messageName) {
    Descriptors.Descriptor descriptor = protoContainer.getDescriptorByName(messageName);
    Descriptors.FileDescriptor fileDescriptor = descriptor.getFile();
    this.bindProviders.forEach(
        provider -> provider.updateResourceBinding(linkedResource, descriptor));
  }

  public void deleteResourceBinding(String linkedResource) {
    this.bindProviders.forEach(provider -> provider.deleteResourceBinding(linkedResource));
  }

  public Registry.ResourceBinding getResourceBinding(String linkedResource) {
    BindResult bindResult = this.bindProviders.get(0).getResourceBinding(linkedResource);
    return Registry.ResourceBinding.newBuilder()
        .setMessageName(bindResult.getMessageName())
        .setLinkedResource(bindResult.getLinkedResource())
        .build();
  }

  public Collection<Registry.ResourceBinding> listResourceBindings(String nextPagetoken) {
    List<BindResult> bindResults = this.bindProviders.get(0).listResourceBindings(nextPagetoken);
    List<Registry.ResourceBinding> bindings = new ArrayList<>(bindResults.size());
    bindResults.forEach(
        result -> {
          bindings.add(
              Registry.ResourceBinding.newBuilder()
                  .setLinkedResource(result.getLinkedResource())
                  .setMessageName(result.getMessageName())
                  .build());
        });
    return bindings;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getUri() {
    return null;
  }

  private <T> T loadProvider(Class<T> clazz, String className) {
    try {
      T provider = (T) Class.forName(className).getConstructor().newInstance();
      return provider;
    } catch (Exception e) {
      throw new RuntimeException("Error loading provider " + className, e);
    }
  }

  void notifyEventListeners(Report report) {
    eventingProviders.forEach(
        provider -> {
          provider.descriptorsChanged(report);
        });
  }
}
