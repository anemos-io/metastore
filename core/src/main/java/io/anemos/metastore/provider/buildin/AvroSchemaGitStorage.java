package io.anemos.metastore.provider.buildin;

import com.google.protobuf.ByteString;
import io.anemos.metastore.core.git.GitConfig;
import io.anemos.metastore.provider.RegistryInfo;
import io.anemos.metastore.provider.StorageProvider;
import java.util.Map;

public class AvroSchemaGitStorage implements StorageProvider {

  private String rootOptionName;
  private GitConfig gitConfig;

  @Override
  public void initForStorage(
      RegistryInfo registryInfo, Map<String, String> config, boolean writeOnly) {
    if (config.get("root") == null) {
      throw new RuntimeException("Root annotation needs to be set.");
    }
    rootOptionName = config.get("root");
    gitConfig = GitConfig.fromMap(config);
    if (!gitConfig.isGitEnabled()) {
      throw new RuntimeException("Git needs to be configured in provider");
    }
  }

  @Override
  public ByteString read() {
    throw new IllegalStateException("Provider only support write");
  }

  @Override
  public void write(ByteString payload) {
    System.out.println();
    // to ProtoDomain
    // get roots from annotation
    // write to git
  }
}
