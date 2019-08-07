package io.anemos.metastore.provider;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GoogleCloudStorage implements StorageProvider, BindProvider {

  private Storage storage;
  private String bucket;
  private String project;
  private String fileName;

  private BindDatabase bindDatabase;

  public void init(RegistryInfo registryInfo, Map<String, String> config, String extension) {
    this.storage = StorageOptions.getDefaultInstance().getService();

    if (config.get("bucket") == null) {
      throw new RuntimeException("bucket variable not set");
    }
    if (config.get("path") == null) {
      throw new RuntimeException("path variable not set");
    }
    if (config.get("project") == null) {
      throw new RuntimeException("project variable not set");
    }

    this.bucket = config.get("bucket");
    this.project = config.get("project");
    if (config.get("path").endsWith("/")) {
      this.fileName = config.get("path") + registryInfo.getName() + "." + extension;
    } else {
      this.fileName = config.get("path") + "/" + registryInfo.getName() + "." + extension;
    }
  }

  @Override
  public void initForStorage(RegistryInfo registryInfo, Map<String, String> config) {
    init(registryInfo, config, "pb");
  }

  @Override
  public void initForBind(
      RegistryInfo registryInfo, Map<String, String> config, boolean writeOnly) {
    init(registryInfo, config, "bind");
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
    storage.create(BlobInfo.newBuilder(bucket, fileName).build(), payload.toByteArray());
  }

  @Override
  public void createResourceBinding(String resourceUrn, Descriptors.Descriptor descriptor) {
    bindDatabase.bind(resourceUrn, descriptor.getFullName());
    saveBind();
  }

  @Override
  public void updateResourceBinding(String resourceUrn, Descriptors.Descriptor descriptor) {
    bindDatabase.bind(resourceUrn, descriptor.getFullName());
    saveBind();
  }

  @Override
  public void deleteResourceBinding(String resourceUrn) {
    bindDatabase.remove(resourceUrn);
    saveBind();
  }

  @Override
  public List<BindResult> listResourceBindings(String next_page_token) {
    return bindDatabase.list(next_page_token);
  }

  @Override
  public boolean isWriteOnly() {
    return false;
  }

  @Override
  public BindResult getResourceBinding(String resourceUrn) {
    try {
      loadBind();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return bindDatabase.get(resourceUrn);
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
