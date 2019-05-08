package io.anemos.metastore;

import com.google.cloud.storage.*;
import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.core.proto.shadow.ShadowRegistry;
import io.anemos.metastore.v1alpha1.Report;
import java.io.IOException;
import java.util.logging.Logger;

public class MetaStore {
  private static final Logger logger = Logger.getLogger(MetaStore.class.getName());
  private static Storage storage;

  // [START init]
  static {
    storage = StorageOptions.getDefaultInstance().getService();
  }

  //    MonoRegistry registry;
  public ProtoDescriptor repo;
  public ShadowRegistry shadowRegistry;

  private String bucket;
  private String path;
  private String project;

  /** Create a RouteGuide server listening on {@code port} using {@code featureFile} database. */
  public MetaStore() throws IOException {

    String provider = System.getenv("METASTORE_STORAGE_PROVIDER");
    // TODO create class from provider, use reflection to invoke constructor.
    if (!"io.anemos.metastore.provider.GoogleCloudStorage".equals(provider)) {
      throw new RuntimeException("Unsupported provider");
    }
    bucket = System.getenv("METASTORE_BUCKET");
    path = System.getenv("METASTORE_PATH");
    project = System.getenv("GOOGLE_PROJECT_ID");

    repo = read("default.pb");
    Report shadowDelta = readDelta();
    shadowRegistry = new ShadowRegistry(repo, shadowDelta);
    shadowRegistry.sync(repo);
  }

  public void writeDefault() {
    storage.create(BlobInfo.newBuilder(bucket, path + "default.pb").build(), repo.toByteArray());
  }

  public void writeShadow() {
    storage.create(
        BlobInfo.newBuilder(bucket, path + "shadow.pb").build(),
        shadowRegistry.getDelta().toByteArray());
  }

  private ProtoDescriptor read(String filename) throws IOException {
    BlobId blobId = BlobId.of(bucket, path + filename);
    if (storage.get(blobId) != null && storage.get(blobId).exists()) {
      byte[] buffer = storage.readAllBytes(blobId, Storage.BlobSourceOption.userProject(project));
      return new ProtoDescriptor(buffer);
    } else {
      return new ProtoDescriptor();
    }
  }

  private Report readDelta() {
    BlobId blobId = BlobId.of(bucket, path + "shadow.pb");
    try {
      byte[] buffer = storage.readAllBytes(blobId, Storage.BlobSourceOption.userProject(project));
      return Report.parseFrom(buffer);
    } catch (Exception e) {
      throw new RuntimeException("Failed to read delta from storage", e);
    }
  }
}
