package io.anemos.metastore.config;

import java.io.InputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ConfigTest {

  @Test
  public void testConfig() {
    InputStream inputStream = this.getClass().getResourceAsStream("test_config.yaml");
    ConfigLoader.load(inputStream);
  }
}
