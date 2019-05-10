package io.anemos.metastore.provider;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;

public class GoogleCloudStorageProvider implements MetaStoreStorageProvider {

  private static Storage storage;

  static {
    storage = StorageOptions.getDefaultInstance().getService();
  }

  private String bucket;
  private String path;
  private String project;

  public GoogleCloudStorageProvider() {
    this.bucket = System.getenv("METASTORE_BUCKET");
    this.path = System.getenv("METASTORE_PATH");
    this.project = System.getenv("GOOGLE_PROJECT_ID");

    if (bucket == null) {
      throw new RuntimeException("METASTORE_BUCKET variable not set");
    }
    if (path == null) {
      throw new RuntimeException("METASTORE_PATH variable not set");
    }
    if (project == null) {
      throw new RuntimeException("GOOGLE_PROJECT_ID variable not set");
    }
  }

  @Override
  public ByteString read(String fileName) {
    BlobId blobId = BlobId.of(bucket, path + fileName);
    if (storage.get(blobId) != null && storage.get(blobId).exists()) {
      byte[] buffer = storage.readAllBytes(blobId, Storage.BlobSourceOption.userProject(project));
      return ByteString.copyFrom(buffer);
    } else {
      return ByteString.EMPTY;
    }
  }

  @Override
  public void write(String fileName, ByteString payload) {
    storage.create(BlobInfo.newBuilder(bucket, path + fileName).build(), payload.toByteArray());
  }
}
