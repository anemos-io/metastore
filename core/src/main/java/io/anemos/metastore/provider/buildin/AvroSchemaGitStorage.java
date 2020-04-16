package io.anemos.metastore.provider.buildin;

import com.google.protobuf.ByteString;
import io.anemos.metastore.core.git.GitConfig;
import io.anemos.metastore.provider.RegistryInfo;
import io.anemos.metastore.provider.StorageProvider;
import io.anemos.metastore.putils.ProtoDomain;
import io.opencensus.common.Scope;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import java.io.*;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvroSchemaGitStorage implements StorageProvider {

  private static final Logger LOG = LoggerFactory.getLogger(AvroSchemaGitStorage.class);
  private static final Tracer TRACER = Tracing.getTracer();
  private String rootOptionName;
  private GitConfig gitConfig;
  private RegistryInfo registryInfo;

  @Override
  public void initForStorage(
      RegistryInfo registryInfo, Map<String, String> config, boolean writeOnly) {
    if (config.get("root") == null) {
      throw new RuntimeException("Root annotation needs to be set.");
    }

    rootOptionName = config.get("root");
    gitConfig = GitConfig.fromMap(config);
    this.registryInfo = registryInfo;
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
    try (Scope scope =
        TRACER.spanBuilder("AvroSchemaGitStorage.write").setRecordEvents(true).startScopedSpan()) {
      ProtoDomain protoDomain = ProtoDomain.buildFrom(payload);
      AvroGit.write(registryInfo.getName(), gitConfig, protoDomain, rootOptionName);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
