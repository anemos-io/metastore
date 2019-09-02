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
public class StorageProviderTest {

  @Rule public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule public TemporaryFolder localTempFolder = new TemporaryFolder();

  public static PContainer baseKnownOption() throws IOException {
    InputStream resourceAsStream =
        StorageProviderTest.class.getResourceAsStream("../server/base_known_option.pb");
    return new PContainer(resourceAsStream);
  }

  public static PContainer baseAddMessageOption() throws IOException {
    InputStream resourceAsStream =
        StorageProviderTest.class.getResourceAsStream("../server/base_add_message_option.pb");
    return new PContainer(resourceAsStream);
  }

  @Before
  public void before() {
    environmentVariables.set("DEBUG", "true");
  }

  @Test
  public void inMemoryProviderTest() throws Exception {
    Map<String, String> config = new HashMap<>();

    StorageProvider provider = new InMemoryStorage();
    provider.initForStorage(new DummyRegistryInfo(), config);
    readNewTest(provider);
    writeReadTest(provider);
  }

  @Test
  public void localFileProviderTest() throws Exception {
    File tempFolder = localTempFolder.newFolder("metastore");
    Map<String, String> config = new HashMap<>();
    config.put("path", tempFolder.getPath());

    StorageProvider provider = new LocalFileStorage();
    provider.initForStorage(new DummyRegistryInfo(), config);
    readNewTest(provider);
    writeReadTest(provider);
  }

  @Test
  public void googleCloudStorageProviderTest() throws Exception {
    Map<String, String> config = new HashMap<>();
    config.put("path", System.getenv("TEST_GOOGLE_CLOUD_BUCKET_PATH"));
    config.put("bucket", System.getenv("TEST_GOOGLE_CLOUD_BUCKET"));
    config.put("project", System.getenv("TEST_GOOGLE_CLOUD_PROJECT"));

    clearGcs(config.get("bucket"), config.get("path"));

    StorageProvider provider = new GoogleCloudStorage();
    provider.initForStorage(new DummyRegistryInfo(), config);
    readNewTest(provider);
    writeReadTest(provider);
  }

  private void readNewTest(StorageProvider provider) {
    Assert.assertNull(provider.read());
  }

  private void writeReadTest(StorageProvider provider) throws Exception {
    provider.write(baseKnownOption().toByteString());
    PContainer PContainer = new PContainer(provider.read());
    Assert.assertEquals(baseKnownOption().toFileDescriptorSet(), PContainer.toFileDescriptorSet());
  }

  private void clearGcs(String bucket, String path) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    try {
      storage.delete(BlobInfo.newBuilder(bucket, path + "test.pb").build().getBlobId());
    } catch (Exception e) {
      //
    }
  }
}
