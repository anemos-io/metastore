package io.anemos.metastore.core.registry;

import com.google.protobuf.ByteString;
import io.anemos.metastore.config.GitGlobalConfig;
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
      StorageProvider storageProvider,
      Registries registries,
      RegistryConfig config,
      GitGlobalConfig global) {
    super(storageProvider, registries, config, global);
    this.shadowOf = config.shadowOf;
  }

  @Override
  public void init() {
    if (read()) {
      write();
    }
    updateShadowCache();
    initGitRepo();
    syncGitRepo("Initialising repository");
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
  public PContainer ref() {
    return registries.get(shadowOf).get();
  }

  @Override
  public boolean isShadow() {
    return true;
  }

  @Override
  public void update(Report report, PContainer in) {
    this.delta = report;
    update();
  }

  @Override
  public void update() {
    write();
    updateShadowCache();
    syncGitRepo("Updated.");
  }

  private void write() {
    storageProvider.write(name + ".pb", raw());
  }

  private boolean read() {
    try {
      ByteString buffer = storageProvider.read(name + ".pb");
      if (buffer == null) {
        delta = Report.parseFrom(ByteString.EMPTY);
        return true;
      } else {
        delta = Report.parseFrom(buffer);
        return false;
      }
    } catch (IOException e) {
      throw new RuntimeException("failed to read shadowCache.pb", e);
    }
  }
}
