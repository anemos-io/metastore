package io.anemos.metastore.core.registry;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.core.proto.PContainer;
import io.anemos.metastore.provider.StorageProvider;
import io.anemos.metastore.v1alpha1.Report;
import java.io.IOException;
import java.util.logging.Logger;

class ShadowRegistry extends AbstractRegistry {

  private static final Logger LOG = Logger.getLogger(ShadowRegistry.class.getName());

  private Report delta;
  private String shadowOf;

  public ShadowRegistry(
      StorageProvider storageProvider, Registries registries, RegistryConfig config)
      throws InvalidProtocolBufferException {
    super(storageProvider, registries, config);
    this.shadowOf = config.shadowOf;
  }

  @Override
  public void init() {
    read();
    updateShadowCache();
    initGitRepo();
    syncGitRepo();
  }

  private void updateShadowCache() {
    PContainer original = registries.get(shadowOf).get();
    protoContainer = new ShadowApply().applyDelta(original, this.delta);
    protoContainer.registerOptions();
  }

  @Override
  public ByteString raw() {
    return delta.toByteString();
  }

  @Override
  public PContainer get() {
    return protoContainer;
  }

  @Override
  public void update(Report report, PContainer in) {
    this.delta = report;
    update();
  }

  @Override
  public void update() {
    storageProvider.write(name + ".pb", raw());
    updateShadowCache();
    syncGitRepo();
  }

  private void read() {
    try {
      ByteString buffer = storageProvider.read(name + ".pb");
      if (buffer == null) {
        delta = Report.parseFrom(ByteString.EMPTY);
      } else {
        delta = Report.parseFrom(buffer);
      }
    } catch (IOException e) {
      throw new RuntimeException("failed to read shadowCache.pb", e);
    }
  }
}
