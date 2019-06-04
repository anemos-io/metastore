package io.anemos.metastore.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class ConfigLoader {

  public static MetaStoreConfig load(InputStream inputStream) {
    Yaml yaml = new Yaml(new Constructor(MetaStoreConfig.class));
    MetaStoreConfig config = yaml.load(inputStream);
    return config;
  }

  public static MetaStoreConfig load(String configPath) {
    try (InputStream inputStream = new FileInputStream(configPath)) {
      return load(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
