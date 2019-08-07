package io.anemos.metastore.core.registry;

import com.google.protobuf.ByteString;
import io.anemos.metastore.config.GitGlobalConfig;
import io.anemos.metastore.config.MetaStoreConfig;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.core.proto.PContainer;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.v1alpha1.Report;
import java.io.IOException;
import java.util.logging.Logger;

class ShadowRegistry extends AbstractRegistry {

  private static final Logger LOG = Logger.getLogger(ShadowRegistry.class.getName());

  private Report delta;
  private String shadowOf;

  public ShadowRegistry(
      Registries registries,
      MetaStoreConfig config,
      RegistryConfig registryConfig,
      GitGlobalConfig global) {
    super(registries, config, registryConfig, global);
    this.shadowOf = registryConfig.shadowOf;
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
  public void update(PContainer ref, PContainer in) {
    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(ref, in, results);
    if (registryConfig.scope != null) {
      for (String packagePrefix : registryConfig.scope) {
        diff.diffOnPackagePrefix(packagePrefix);
      }
    } else {
      throw new RuntimeException("Shadow registry should have package prefix scopes defined.");
    }
    delta = results.getReport();
    update();
  }

  @Override
  public void update() {
    write();
    updateShadowCache();
    syncGitRepo("Updated.");
  }

  private void write() {
    storageProvider.write(raw());
  }

  private boolean read() {
    try {
      ByteString buffer = storageProvider.read();
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
