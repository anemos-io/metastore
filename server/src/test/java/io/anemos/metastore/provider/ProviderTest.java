package io.anemos.metastore.provider;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.anemos.metastore.MetaStore;
import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.v1alpha1.Report;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ProviderTest {

  @Rule public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule public TemporaryFolder localTempFolder = new TemporaryFolder();

  @Before
  public void before() throws Exception {
    environmentVariables.set("DEBUG", "true");
  }

  @Test
  public void inMemoryProviderTest() throws Exception {
    environmentVariables.set(
        "METASTORE_STORAGE_PROVIDER", "io.anemos.metastore.provider.InMemoryProvider");
    MetaStore metaStore = new MetaStore();
    readNewTest(metaStore);
    writeReadTest(metaStore);
  }

  @Test
  public void localFileProviderTest() throws Exception {
    File tempFolder = localTempFolder.newFolder("metastore");
    environmentVariables.set("METASTORE_PATH", tempFolder.getPath());
    environmentVariables.set(
        "METASTORE_STORAGE_PROVIDER", "io.anemos.metastore.provider.LocalFileProvider");

    MetaStore metaStore = new MetaStore();
    readNewTest(metaStore);
    writeReadTest(metaStore);
  }

  @Test
  public void googleCloudStorageProviderTest() throws Exception {
    environmentVariables.set("METASTORE_PATH", "metastore-test/");
    environmentVariables.set("METASTORE_BUCKET", "vptech-data-core-test");
    environmentVariables.set("GOOGLE_PROJECT_ID", "vptech-data-core-test");
    environmentVariables.set(
        "METASTORE_STORAGE_PROVIDER", "io.anemos.metastore.provider.GoogleCloudStorageProvider");

    clearGcs(System.getenv("METASTORE_BUCKET"), System.getenv("METASTORE_PATH"));
    MetaStore metaStore = new MetaStore();
    readNewTest(metaStore);
    writeReadTest(metaStore);
  }

  private void readNewTest(MetaStore metaStore) throws Exception {
    Assert.assertEquals(
        new ProtoDescriptor().toFileDescriptorSet(), metaStore.repo.toFileDescriptorSet());
    Assert.assertEquals(Report.getDefaultInstance(), metaStore.shadowRegistry.getDelta());
  }

  private void writeReadTest(MetaStore metaStore) throws Exception {
    metaStore.repo = baseKnownOption();
    metaStore.writeDefault();
    metaStore.shadowRegistry.setDelta(getReport());
    metaStore.writeShadowDelta();

    metaStore.readDefault();
    Assert.assertEquals(
        baseKnownOption().toFileDescriptorSet(), metaStore.repo.toFileDescriptorSet());
    metaStore.readShadowDelta();
    Assert.assertEquals(getReport(), metaStore.shadowRegistry.getDelta());
  }

  public static ProtoDescriptor baseKnownOption() throws IOException {
    InputStream resourceAsStream =
        ProviderTest.class.getResourceAsStream("../base_known_option.pb");
    return new ProtoDescriptor(resourceAsStream);
  }

  public static ProtoDescriptor baseAddMessageOption() throws IOException {
    InputStream resourceAsStream =
        ProviderTest.class.getResourceAsStream("../base_add_message_option.pb");
    return new ProtoDescriptor(resourceAsStream);
  }

  public Report getReport() throws Exception {
    ProtoDescriptor baseAddMessageOption = baseAddMessageOption();
    ProtoDescriptor base = baseKnownOption();

    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(base, baseAddMessageOption, results);
    diff.diffOnMessage("test.v1.ProtoBeamBasicMessage");
    return results.getReport();
  }

  private void clearGcs(String bucket, String path) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    try {
      storage.delete(BlobInfo.newBuilder(bucket, path + "shadow.pb").build().getBlobId());
      storage.delete(BlobInfo.newBuilder(bucket, path + "default.pb").build().getBlobId());
    } catch (Exception e) {
      //
    }
  }
}
