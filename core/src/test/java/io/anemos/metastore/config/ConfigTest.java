package io.anemos.metastore.config;

import static org.junit.Assert.assertEquals;

import com.jcraft.jsch.HostKey;
import io.anemos.metastore.core.git.GitConfig;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ConfigTest {

  private MetaStoreConfig loadConfig(String fileName) {
    InputStream inputStream = this.getClass().getResourceAsStream(fileName);
    return ConfigLoader.load(inputStream).resolve();
  }

  @Test
  public void testGlobalGit() {
    MetaStoreConfig config = loadConfig("test_config.yaml");
    GitGlobalConfig globalGit = config.getGlobalGit();
    assertEquals("cHJpdmF0ZQo=", globalGit.privateKey);
    assertEquals(1, globalGit.hosts.length);
    assertEquals("[host.example.com]", globalGit.hosts[0].host);
    assertEquals("cHVibGljCg==", globalGit.hosts[0].key);
    assertEquals("ecdsa-sha2-nistp256", globalGit.hosts[0].type);
  }

  @Test
  public void resolvedGitConfig() {
    MetaStoreConfig config = loadConfig("test_config.yaml");
    RegistryConfig registryConfig = config.getRegistryConfig("git");
    GitConfig gitConfig = registryConfig.getGitConfig();

    HostKey hostKey = gitConfig.getHostKeys().get(0);
    assertEquals("cHVibGljCg==", hostKey.getKey());
    assertEquals("[host.example.com]", hostKey.getHost());
    assertEquals("ecdsa-sha2-nistp256", hostKey.getType());
  }

  @Test
  public void testMinimum() {
    MetaStoreConfig config = loadConfig("test_minimum.yaml");
    RegistryConfig registryConfig = config.getRegistryConfig("default");
    List<ProviderConfig> storage = registryConfig.getStorage();
    assertEquals(1, storage.size());
    assertEquals("io.anemos.metastore.provider.InMemoryStorage", storage.get(0).getProviderClass());
  }
}
