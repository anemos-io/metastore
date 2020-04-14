package io.anemos.metastore.core.registry;

import com.google.protobuf.ByteString;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.provider.StorageProvider;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.Patch;
import io.anemos.metastore.v1alpha1.RegistryP;
import java.io.IOException;

class SchemaRegistry extends AbstractRegistry {

  public SchemaRegistry(Registries registries, RegistryConfig registryConfig) {
    super(registries, registryConfig);
  }

  @Override
  public void init() {
    if (read()) {
      write();
    } else {
      writeWriteOnly();
    }
    initGitRepo();
    syncGitRepo(RegistryP.Note.newBuilder().setNote("(Re)Sync repo").build());
  }

  @Override
  public ByteString raw() {
    return protoContainer.toByteString();
  }

  @Override
  public ProtoDomain get() {
    return protoContainer;
  }

  @Override
  public ProtoDomain ref() {
    return protoContainer;
  }

  @Override
  public void update(ProtoDomain ref, ProtoDomain in, Patch patch, RegistryP.Note note) {
    protoContainer = in;
    update(note);
    syncGitRepo(note);
    notifyEventListeners(patch);
  }

  @Override
  public void update(RegistryP.Note note) {
    write();
    registries.notifyShadows(getName(), note);
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
        this.protoContainer = ProtoDomain.empty();
        return true;
      } else {
        this.protoContainer = ProtoDomain.buildFrom(buffer);
        return false;
      }
    } catch (IOException e) {
      throw new RuntimeException("failed to read default.pb", e);
    }
  }
}
