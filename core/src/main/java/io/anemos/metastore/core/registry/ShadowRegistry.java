package io.anemos.metastore.core.registry;

import com.google.protobuf.ByteString;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.provider.StorageProvider;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.Patch;
import io.anemos.metastore.v1alpha1.RegistryP;
import io.anemos.metastore.v1alpha1.Report;
import io.grpc.StatusException;
import java.io.IOException;

class ShadowRegistry extends AbstractRegistry {
  private Patch patch;
  private String shadowOf;

  public ShadowRegistry(Registries registries, RegistryConfig registryConfig) {
    super(registries, registryConfig);
    this.shadowOf = registryConfig.getShadowOf();
  }

  @Override
  public void init() {
    if (read()) {
      write();
    } else {
      writeWriteOnly();
    }
    updateShadowCache();
    initGitRepo();
    syncGitRepo(RegistryP.Note.newBuilder().setNote("(Re)Sync repo").build());
  }

  private void updateShadowCache() {
    ProtoDomain original = null;
    try {
      original = registries.get(shadowOf).get();
    } catch (StatusException e) {
      throw new RuntimeException("Unable to find registry with name " + shadowOf);
    }
    protoContainer = new ShadowApply().applyDelta(original, this.patch);
    protoContainer.registerOptions();
  }

  @Override
  public ByteString raw() {
    return patch.toByteString();
  }

  @Override
  public ProtoDomain get() {
    return protoContainer;
  }

  @Override
  public ProtoDomain ref() {
    try {
      return registries.get(shadowOf).get();
    } catch (StatusException e) {
      throw new RuntimeException("Unable to find registry with name " + shadowOf);
    }
  }

  @Override
  public void update(ProtoDomain ref, ProtoDomain in, Report report, RegistryP.Note note) {
    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(ref, in, results);
    if (registryConfig.getScope() != null) {
      for (String packagePrefix : registryConfig.getScope()) {
        diff.diffOnPackagePrefix(packagePrefix);
      }
    } else {
      throw new RuntimeException("Shadow registry should have package prefix scopes defined.");
    }
    patch = results.createProto();
    update(note);
    notifyEventListeners(report);
  }

  @Override
  public void update(RegistryP.Note note) {
    write();
    updateShadowCache();
    syncGitRepo(note);
  }

  private void write() {
    for (StorageProvider storageProvider : storageProviders) {
      storageProvider.write(raw());
    }
  }

  private void writeWriteOnly() {
    for (int i = 1; i < storageProviders.size(); i++) {
      storageProviders.get(i).write(raw());
    }
  }

  private boolean read() {
    try {
      ByteString buffer = storageProviders.get(0).read();
      if (buffer == null) {
        patch = Patch.parseFrom(ByteString.EMPTY);
        return true;
      } else {
        patch = Patch.parseFrom(buffer);
        return false;
      }
    } catch (IOException e) {
      throw new RuntimeException("failed to read shadowCache.pb", e);
    }
  }
}
