package io.anemos.metastore.provider;

import com.google.cloud.ServiceOptions;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import io.opencensus.common.Scope;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleCloudStorage implements StorageProvider, BindProvider {

  private static final Logger LOG = LoggerFactory.getLogger(GoogleCloudStorage.class);
  private static final Tracer TRACER = Tracing.getTracer();
  private Storage storage;
  private String bucket;
  private String project;
  private String fileName;

  private BindDatabase bindDatabase;

  public void init(RegistryInfo registryInfo, Map<String, String> config, String extension) {
    this.storage = StorageOptions.getDefaultInstance().getService();
    String project = ServiceOptions.getDefaultProjectId();

    if (config.get("project") == null && project == null) {
      throw new RuntimeException("project variable not set");
    }
    if (config.get("bucket") == null) {
      throw new RuntimeException("bucket variable not set");
    }
    if (config.get("path") == null) {
      throw new RuntimeException("path variable not set");
    }

    if (config.get("project") != null) {
      this.project = config.get("project");
    }
    this.bucket = config.get("bucket");
    if (config.get("path").endsWith("/")) {
      this.fileName = config.get("path") + registryInfo.getName() + "." + extension;
    } else {
      this.fileName = config.get("path") + "/" + registryInfo.getName() + "." + extension;
    }
  }

  @Override
  public void initForStorage(
      RegistryInfo registryInfo, Map<String, String> config, boolean writeOnly) {
    init(registryInfo, config, "pb");
  }

  @Override
  public void initForBind(
      RegistryInfo registryInfo, Map<String, String> config, boolean writeOnly) {
    bindDatabase = new BindDatabase();
    init(registryInfo, config, "bind");
    try {
      loadBind();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public ByteString read() {
    BlobId blobId = BlobId.of(bucket, fileName);
    if (storage.get(blobId) != null && storage.get(blobId).exists()) {
      byte[] buffer = storage.readAllBytes(blobId, Storage.BlobSourceOption.userProject(project));
      return ByteString.copyFrom(buffer);
    } else {
      return null;
    }
  }

  @Override
  public void write(ByteString payload) {
    try (Scope scope =
        TRACER.spanBuilder("GoogleCloudStorage.write").setRecordEvents(true).startScopedSpan()) {
      storage.create(BlobInfo.newBuilder(bucket, fileName).build(), payload.toByteArray());
    }
  }

  @Override
  public void createResourceBinding(String resourceUrn, Descriptors.Descriptor descriptor) {
    try (Scope scope =
        TRACER
            .spanBuilder("GoogleCloudStorage.createResourceBinding")
            .setRecordEvents(true)
            .startScopedSpan()) {
      bindDatabase.bindMessage(resourceUrn, descriptor.getFullName());
      saveBind();
    }
  }

  @Override
  public void updateResourceBinding(String resourceUrn, Descriptors.Descriptor descriptor) {
    try (Scope scope =
        TRACER
            .spanBuilder("GoogleCloudStorage.updateResourceBinding")
            .setRecordEvents(true)
            .startScopedSpan()) {
      bindDatabase.bindMessage(resourceUrn, descriptor.getFullName());
      saveBind();
    }
  }

  @Override
  public void createServiceBinding(String resourceUrn, Descriptors.ServiceDescriptor descriptor) {
    try (Scope scope =
        TRACER
            .spanBuilder("GoogleCloudStorage.createServiceBinding")
            .setRecordEvents(true)
            .startScopedSpan()) {
      bindDatabase.bindService(resourceUrn, descriptor.getFullName());
      saveBind();
    }
  }

  @Override
  public void updateServiceBinding(String resourceUrn, Descriptors.ServiceDescriptor descriptor) {
    try (Scope scope =
        TRACER
            .spanBuilder("GoogleCloudStorage.updateServiceBinding")
            .setRecordEvents(true)
            .startScopedSpan()) {
      bindDatabase.bindService(resourceUrn, descriptor.getFullName());
      saveBind();
    }
  }

  @Override
  public void deleteResourceBinding(String resourceUrn) {
    try (Scope scope =
        TRACER
            .spanBuilder("GoogleCloudStorage.deleteResourceBinding")
            .setRecordEvents(true)
            .startScopedSpan()) {
      bindDatabase.remove(resourceUrn);
      saveBind();
    }
  }

  @Override
  public List<BindResult> listResourceBindings(String next_page_token) {
    try (Scope scope =
        TRACER
            .spanBuilder("GoogleCloudStorage.listResourceBindings")
            .setRecordEvents(true)
            .startScopedSpan()) {
      return bindDatabase.list(next_page_token);
    }
  }

  @Override
  public boolean isWriteOnly() {
    return false;
  }

  @Override
  public BindResult getResourceBinding(String resourceUrn) {
    try (Scope scope =
        TRACER
            .spanBuilder("GoogleCloudStorage.getResourceBinding")
            .setRecordEvents(true)
            .startScopedSpan()) {
      return bindDatabase.get(resourceUrn);
    }
  }

  private void saveBind() {
    storage.create(BlobInfo.newBuilder(bucket, fileName).build(), bindDatabase.toByteArray());
  }

  private void loadBind() throws IOException {
    BlobId blobId = BlobId.of(bucket, fileName);
    if (storage.get(blobId) != null && storage.get(blobId).exists()) {
      byte[] buffer = storage.readAllBytes(blobId, Storage.BlobSourceOption.userProject(project));
      bindDatabase.parse(buffer);
    }
  }
}
