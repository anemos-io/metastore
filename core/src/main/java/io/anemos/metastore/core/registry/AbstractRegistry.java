package io.anemos.metastore.core.registry;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import io.anemos.metastore.config.ProviderConfig;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.provider.BindProvider;
import io.anemos.metastore.provider.BindResult;
import io.anemos.metastore.provider.EventingProvider;
import io.anemos.metastore.provider.RegistryInfo;
import io.anemos.metastore.provider.StorageProvider;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.BindP;
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
  private List<BindProvider> bindProviders;
  protected List<EventingProvider> eventingProviders;
  protected List<StorageProvider> storageProviders;

  RegistryConfig registryConfig;
  ProtoDomain protoContainer;
  private MetaGit metaGit;

  AbstractRegistry(Registries registries, RegistryConfig registryConfig) {
    this.registries = registries;
    this.registryConfig = registryConfig;
    this.metaGit = new MetaGit(getName(), registryConfig.getGitConfig());
    this.storageProviders = new ArrayList<>();
    this.bindProviders = new ArrayList<>();
    this.eventingProviders = new ArrayList<>();

    boolean writeOnly = false;
    for (ProviderConfig provider : registryConfig.getStorage()) {
      StorageProvider bindProvider =
          loadProvider(StorageProvider.class, provider.getProviderClass());
      bindProvider.initForStorage(this, provider.getParameter(), writeOnly);
      storageProviders.add(bindProvider);
    }
    writeOnly = false;
    for (ProviderConfig provider : registryConfig.getBind()) {
      BindProvider bindProvider = loadProvider(BindProvider.class, provider.getProviderClass());
      bindProvider.initForBind(this, provider.getParameter(), writeOnly);
      writeOnly = true;
      bindProviders.add(bindProvider);
    }
    for (ProviderConfig provider : registryConfig.getEventing()) {
      EventingProvider eventingProvider =
          loadProvider(EventingProvider.class, provider.getProviderClass());
      eventingProvider.initForChangeEvent(this, provider.getParameter());
      eventingProviders.add(eventingProvider);
    }
  }

  public abstract void init();

  public abstract void update(Comment comment);

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

  public void updateResourceBinding(BindP.ResourceBinding resourceBinding, boolean create)
      throws StatusException {
    if (resourceBinding == null) {
      throw Status.INVALID_ARGUMENT.withDescription("binding should be set.").asException();
    }

    String linkedResource = validateLinkedResource(resourceBinding.getLinkedResource());

    if (resourceBinding.getTypeCase().getNumber()
        == BindP.ResourceBinding.MESSAGE_NAME_FIELD_NUMBER) {
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

  public BindP.ResourceBinding getResourceBinding(String linkedResource) throws StatusException {
    BindResult bindResult = this.bindProviders.get(0).getResourceBinding(linkedResource);
    if (bindResult == null) {
      throw Status.NOT_FOUND
          .withDescription("No binding for the linked_resource is found.")
          .asException();
    }
    return toResourceBinding(bindResult);
  }

  public Collection<BindP.ResourceBinding> listResourceBindings(String nextPagetoken) {
    List<BindResult> bindResults = this.bindProviders.get(0).listResourceBindings(nextPagetoken);
    List<BindP.ResourceBinding> bindings = new ArrayList<>(bindResults.size());
    bindResults.forEach(
        result -> {
          bindings.add(toResourceBinding(result));
        });
    return bindings;
  }

  private BindP.ResourceBinding toResourceBinding(BindResult result) {
    BindP.ResourceBinding.Builder resourceBinding = BindP.ResourceBinding.newBuilder();
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
    return registryConfig.getName();
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
