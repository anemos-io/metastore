package io.anemos.metastore.core.registry;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import io.anemos.metastore.config.GitGlobalConfig;
import io.anemos.metastore.config.MetaStoreConfig;
import io.anemos.metastore.config.ProviderConfig;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.provider.BindProvider;
import io.anemos.metastore.provider.BindResult;
import io.anemos.metastore.provider.EventingProvider;
import io.anemos.metastore.provider.RegistryInfo;
import io.anemos.metastore.provider.StorageProvider;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.RegistryP;
import io.anemos.metastore.v1alpha1.RegistryP.SubmitSchemaRequest.Comment;
import io.anemos.metastore.v1alpha1.Report;
import io.grpc.Status;
import io.grpc.StatusException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRegistry implements RegistryInfo {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractRegistry.class);
  protected final Registries registries;
  protected final String name;
  private List<BindProvider> bindProviders;
  protected List<EventingProvider> eventingProviders;
  MetaStoreConfig config;
  RegistryConfig registryConfig;
  final StorageProvider storageProvider;
  ProtoDomain protoContainer;
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
      LOG.warn("Storage Provider not configured, defaulting to in memory provider");
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

  public abstract ProtoDomain get();

  public abstract ProtoDomain ref();

  public abstract void update(ProtoDomain ref, ProtoDomain in, Report report, Comment comment);

  void syncGitRepo(Comment comment) {
    metaGit.sync(protoContainer, comment);
  }

  void initGitRepo() {
    metaGit.init();
  }

  public void updateResourceBinding(RegistryP.ResourceBinding resourceBinding, boolean create)
      throws StatusException {
    if (resourceBinding == null) {
      throw Status.INVALID_ARGUMENT.withDescription("binding should be set.").asException();
    }

    String linkedResource = validateLinkedResource(resourceBinding.getLinkedResource());

    if (resourceBinding.getTypeCase().getNumber()
        == RegistryP.ResourceBinding.MESSAGE_NAME_FIELD_NUMBER) {
      Descriptors.Descriptor descriptor =
          protoContainer.getDescriptorByName(resourceBinding.getMessageName());
      if (descriptor == null) {
        throw Status.NOT_FOUND
            .withDescription("The descriptor with message_name is not found in the registry.")
            .asException();
      }
      this.bindProviders.forEach(
          provider -> {
            if (create) {
              provider.createResourceBinding(linkedResource, descriptor);
            } else {
              provider.updateResourceBinding(linkedResource, descriptor);
            }
          });
    } else if (resourceBinding.getServiceName() != null) {
      Descriptors.ServiceDescriptor descriptor =
          protoContainer.getServiceDescriptorByName(resourceBinding.getServiceName());
      if (descriptor == null) {
        throw Status.NOT_FOUND
            .withDescription("The descriptor with service_name is not found in the registry.")
            .asException();
      }
      this.bindProviders.forEach(
          provider -> {
            if (create) {
              provider.createServiceBinding(linkedResource, descriptor);
            } else {
              provider.updateServiceBinding(linkedResource, descriptor);
            }
          });
    } else {
      throw Status.INVALID_ARGUMENT
          .withDescription("Either message_name or service_name should be specified.")
          .asException();
    }
  }

  public void deleteResourceBinding(String linkedResource) {
    this.bindProviders.forEach(provider -> provider.deleteResourceBinding(linkedResource));
  }

  public RegistryP.ResourceBinding getResourceBinding(String linkedResource)
      throws StatusException {
    BindResult bindResult = this.bindProviders.get(0).getResourceBinding(linkedResource);
    if (bindResult == null) {
      throw Status.NOT_FOUND
          .withDescription("No binding for the linked_resource is found.")
          .asException();
    }
    return toResourceBinding(bindResult);
  }

  public Collection<RegistryP.ResourceBinding> listResourceBindings(String nextPagetoken) {
    List<BindResult> bindResults = this.bindProviders.get(0).listResourceBindings(nextPagetoken);
    List<RegistryP.ResourceBinding> bindings = new ArrayList<>(bindResults.size());
    bindResults.forEach(
        result -> {
          bindings.add(toResourceBinding(result));
        });
    return bindings;
  }

  private RegistryP.ResourceBinding toResourceBinding(BindResult result) {
    RegistryP.ResourceBinding.Builder resourceBinding = RegistryP.ResourceBinding.newBuilder();
    resourceBinding.setLinkedResource(result.getLinkedResource());
    if (result.getMessageName() != null) {
      resourceBinding.setMessageName(result.getMessageName());
    } else if (result.getServiceName() != null) {
      resourceBinding.setServiceName(result.getServiceName());
    } else {

    }
    return resourceBinding.build();
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

  String validateLinkedResource(String linkedResource) throws StatusException {
    if (linkedResource == null) {
      throw Status.INVALID_ARGUMENT
          .withDescription("linked_resource should not be empty.")
          .asException();
    }
    URI uri = URI.create(linkedResource);
    if (uri == null) {
      throw Status.INVALID_ARGUMENT
          .withDescription("linked_resource is not a valid URI.")
          .asException();
    }
    if (uri.getScheme() == null) {
      throw Status.INVALID_ARGUMENT
          .withDescription("linked_resource is not a valid URI, unable to determine schema.")
          .asException();
    }
    switch (uri.getScheme()) {
      case "http":
      case "https":
      case "googlecloud":
        return linkedResource;
      default:
        throw Status.INVALID_ARGUMENT
            .withDescription(
                "Invalid linked_resource uri scheme. Only http, https and googlecloud is supported.")
            .asException();
    }
  }
}
