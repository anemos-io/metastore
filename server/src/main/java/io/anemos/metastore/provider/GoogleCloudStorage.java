package io.anemos.metastore.provider;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import java.util.Map;

public class GoogleCloudStorage implements StorageProvider {

  private static Storage storage;

  static {
    storage = StorageOptions.getDefaultInstance().getService();
  }

  private String bucket;
  private String path;
  private String project;

  public GoogleCloudStorage(Map<String, String> config) {
    this.bucket = config.get("bucket");
    this.path = config.get("path");
    this.project = config.get("project");

    if (bucket == null) {
      throw new RuntimeException("bucket variable not set");
    }
    if (path == null) {
      throw new RuntimeException("path variable not set");
    }
    if (project == null) {
      throw new RuntimeException("project variable not set");
    }
  }

  @Override
  public ByteString read(String fileName) {
    BlobId blobId = BlobId.of(bucket, path + fileName);
    if (storage.get(blobId) != null && storage.get(blobId).exists()) {
      byte[] buffer = storage.readAllBytes(blobId, Storage.BlobSourceOption.userProject(project));
      return ByteString.copyFrom(buffer);
    } else {
      return null;
    }
  }

  @Override
  public void write(String fileName, ByteString payload) {
    storage.create(BlobInfo.newBuilder(bucket, path + fileName).build(), payload.toByteArray());
  }
}
