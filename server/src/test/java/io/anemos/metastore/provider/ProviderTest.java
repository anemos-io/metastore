package io.anemos.metastore.provider;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.anemos.metastore.core.proto.PContainer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
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

  public static PContainer baseKnownOption() throws IOException {
    InputStream resourceAsStream =
        ProviderTest.class.getResourceAsStream("../server/base_known_option.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseAddMessageOption() throws IOException {
    InputStream resourceAsStream =
        ProviderTest.class.getResourceAsStream("../server/base_add_message_option.pb");
    return new PContainer(resourceAsStream);
  }

  @Before
  public void before() {
    environmentVariables.set("DEBUG", "true");
  }

  @Test
  public void inMemoryProviderTest() throws Exception {
    Map<String, String> config = new HashMap<>();

    StorageProvider provider = new InMemoryProvider(config);
    readNewTest(provider);
    writeReadTest(provider);
  }

  @Test
  public void localFileProviderTest() throws Exception {
    File tempFolder = localTempFolder.newFolder("metastore");
    Map<String, String> config = new HashMap<>();
    config.put("path", tempFolder.getPath());

    StorageProvider provider = new LocalFileProvider(config);
    readNewTest(provider);
    writeReadTest(provider);
  }

  @Test
  public void googleCloudStorageProviderTest() throws Exception {
    Map<String, String> config = new HashMap<>();
    config.put("path", "metastore-test/");
    config.put("bucket", "vptech-data-core-test");
    config.put("project", "vptech-data-core-test");

    clearGcs(config.get("bucket"), config.get("path"));

    StorageProvider provider = new GoogleCloudStorageProvider(config);
    readNewTest(provider);
    writeReadTest(provider);
  }

  private void readNewTest(StorageProvider provider) throws Exception {
    Assert.assertNull(provider.read("default.pb"));
  }

  private void writeReadTest(StorageProvider provider) throws Exception {
    provider.write("default.pb", baseKnownOption().toByteString());
    PContainer PContainer = new PContainer(provider.read("default.pb"));
    Assert.assertEquals(
        baseKnownOption().toFileDescriptorSet(), PContainer.toFileDescriptorSet());
  }

  private void clearGcs(String bucket, String path) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    try {
      storage.delete(BlobInfo.newBuilder(bucket, path + "default.pb").build().getBlobId());
    } catch (Exception e) {
      //
    }
  }
}
