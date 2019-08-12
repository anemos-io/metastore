package io.anemos.metastore.provider;

import com.google.cloud.datacatalog.CreateTagRequest;
import com.google.cloud.datacatalog.Tag;
import com.google.cloud.datacatalog.v1beta1.DataCatalogClient;
import com.google.protobuf.Descriptors;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GoogleDataCatalog implements BindProvider {
  private DataCatalogClient dataCatalogClient;

  @Override
  public void initForBind(
      RegistryInfo registryInfo, Map<String, String> config, boolean writeOnly) {
    try {
      dataCatalogClient = DataCatalogClient.create();
    } catch (IOException e) {
      throw new RuntimeException("Can't initialise the DataCatalogClient", e);
    }
  }

  @Override
  public void createResourceBinding(String resourceUrn, Descriptors.Descriptor descriptor) {
    dataCatalogClient.createTag(
        CreateTagRequest.newBuilder()
            .setParent("")
            .setTag(Tag.newBuilder().setName("xxx").build())
            .build());
  }

  @Override
  public void updateResourceBinding(String resourceUrn, Descriptors.Descriptor descriptor) {}

  @Override
  public void createServiceBinding(String resourceUrn, Descriptors.ServiceDescriptor descriptor) {}

  @Override
  public void updateServiceBinding(String resourceUrn, Descriptors.ServiceDescriptor descriptor) {}

  @Override
  public void deleteResourceBinding(String resourceUrn) {}

  @Override
  public List<BindResult> listResourceBindings(String next_page_token) {
    throw new RuntimeException("This is a read only provider, this should never be called.");
  }

  @Override
  public BindResult getResourceBinding(String resourceUrn) {
    throw new RuntimeException("This is a read only provider, this should never be called.");
  }

  @Override
  public boolean isWriteOnly() {
    return true;
  }
}
