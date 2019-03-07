package io.anemos.metastore;

import com.google.cloud.storage.*;
import io.anemos.metastore.core.proto.ProtoDescriptor;

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

    private String bucket;
    private String path;
    private String project;


    /**
     * Create a RouteGuide server listening on {@code port} using {@code featureFile} database.
     */
    public MetaStore() throws IOException {

        String provider = System.getenv("METASTORE_STORAGE_PROVIDER");
        if (!"io.amemos.metastore.provider.GoogleCloudStorage".equals(provider)) {
            throw new RuntimeException("Unsupported provider");
        }
        bucket = System.getenv("METASTORE_BUCKET");
        path = System.getenv("METASTORE_PATH");
        path = System.getenv("METASTORE_PATH");
        project = System.getenv("GOOGLE_PROJECT_ID");

        read();
    }

    public void write() {
        Blob blob = storage.create(BlobInfo
                        .newBuilder(bucket, path + "default.pb")
                        .build(),
                repo.toByteArray());
    }

    private void read() throws IOException {
        BlobId blobId = BlobId.of(bucket, path + "default.pb");
        if (storage.get(blobId).exists()) {
            byte[] buffer = storage.readAllBytes(blobId, Storage.BlobSourceOption.userProject(project));
            repo = new ProtoDescriptor(buffer);
        } else {
            repo = new ProtoDescriptor();
        }
    }


}