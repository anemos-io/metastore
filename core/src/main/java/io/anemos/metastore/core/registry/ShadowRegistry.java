package io.anemos.metastore.core.registry;

import com.google.protobuf.ByteString;
import io.anemos.metastore.config.GitGlobalConfig;
import io.anemos.metastore.config.MetaStoreConfig;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.RegistryP.SubmitSchemaRequest.Comment;
import io.anemos.metastore.v1alpha1.Report;
import io.grpc.StatusException;
import java.io.IOException;

class ShadowRegistry extends AbstractRegistry {
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
    syncGitRepo(Comment.newBuilder().setDescription("(Re)Sync repo").build());
  }

  private void updateShadowCache() {
    ProtoDomain original = null;
    try {
      original = registries.get(shadowOf).get();
    } catch (StatusException e) {
      throw new RuntimeException("Unable to find registry with name " + shadowOf);
    }
    protoContainer = new ShadowApply().applyDelta(original, this.delta);
    protoContainer.registerOptions();
  }

  @Override
  public ByteString raw() {
    return delta.toByteString();
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
  public void update(ProtoDomain ref, ProtoDomain in, Report report, Comment comment) {
    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(ref, in, results);
    if (registryConfig.scope != null) {
      for (String packagePrefix : registryConfig.scope) {
        diff.diffOnPackagePrefix(packagePrefix);
      }
    } else {
      throw new RuntimeException("Shadow registry should have package prefix scopes defined.");
    }
    delta = results.createProto();
    update(comment);
    notifyEventListeners(report);
  }

  @Override
  public void update(Comment comment) {
    write();
    updateShadowCache();
    syncGitRepo(comment);
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
