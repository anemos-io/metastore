package io.anemos.metastore.provider;

import com.google.cloud.datacatalog.CreateTagRequest;
import com.google.cloud.datacatalog.Tag;
import com.google.cloud.datacatalog.v1beta1.DataCatalogClient;
import com.google.protobuf.Descriptors;
import io.opencensus.common.Scope;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleDataCatalog implements BindProvider {

  private static final Logger LOG = LoggerFactory.getLogger(GoogleDataCatalog.class);
  private static final Tracer TRACER = Tracing.getTracer();
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
    try (Scope scope =
        TRACER
            .spanBuilder("GoogleDataCatalog.createResourceBinding")
            .setRecordEvents(true)
            .startScopedSpan()) {
      dataCatalogClient.createTag(
          CreateTagRequest.newBuilder()
              .setParent("")
              .setTag(Tag.newBuilder().setName("xxx").build())
              .build());
    }
  }

  @Override
  public void updateResourceBinding(String resourceUrn, Descriptors.Descriptor descriptor) {
    try (Scope scope =
        TRACER
            .spanBuilder("GoogleDataCatalog.updateResourceBinding")
            .setRecordEvents(true)
            .startScopedSpan()) {}
  }

  @Override
  public void createServiceBinding(String resourceUrn, Descriptors.ServiceDescriptor descriptor) {
    try (Scope scope =
        TRACER
            .spanBuilder("GoogleDataCatalog.createServiceBinding")
            .setRecordEvents(true)
            .startScopedSpan()) {}
  }

  @Override
  public void updateServiceBinding(String resourceUrn, Descriptors.ServiceDescriptor descriptor) {
    try (Scope scope =
        TRACER
            .spanBuilder("GoogleDataCatalog.updateResourceBinding")
            .setRecordEvents(true)
            .startScopedSpan()) {}
  }

  @Override
  public void deleteResourceBinding(String resourceUrn) {
    try (Scope scope =
        TRACER
            .spanBuilder("GoogleDataCatalog.deleteResourceBinding")
            .setRecordEvents(true)
            .startScopedSpan()) {}
  }

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
