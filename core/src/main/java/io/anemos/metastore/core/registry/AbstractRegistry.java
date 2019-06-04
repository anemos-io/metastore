package io.anemos.metastore.core.registry;

import com.google.protobuf.ByteString;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.provider.StorageProvider;
import io.anemos.metastore.v1alpha1.Report;
import java.io.File;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;

public abstract class AbstractRegistry {

  private static final Logger LOG = Logger.getLogger(AbstractRegistry.class.getName());
  protected final Registries registries;
  protected final String name;
  protected final RegistryConfig config;
  final StorageProvider storageProvider;
  ProtoDescriptor protoContainer;
  private Git gitRepo;

  AbstractRegistry(StorageProvider storageProvider, Registries registries, RegistryConfig config) {
    this.storageProvider = storageProvider;
    this.registries = registries;
    this.config = config;
    this.name = config.name;
  }

  public abstract void init();

  public abstract void update();

  public abstract ByteString raw();

  public abstract ProtoDescriptor get();

  public abstract void update(Report report, ProtoDescriptor in);

  void syncGitRepo() {
    if (config.git == null) {
      return;
    }

    try {
      if (System.getenv("DEBUG") != null && System.getenv("DEBUG").equals("true")) {
        protoContainer.writeToDirectory(new File(config.git.path).toPath().toString());
        return;
      }

      gitRepo.pull();
      protoContainer.writeToDirectory(new File(config.git.path).toPath().toString());
      gitRepo.add().addFilepattern(".").call();
      Status status = gitRepo.status().call();
      if (status.hasUncommittedChanges()) {
        gitRepo.commit().setMessage("shadowCache apply").call();
        gitRepo.push().call();
        LOG.info("shadowCache apply");
      } else {
        LOG.info("no changes to commit");
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed syncing the shadowCache repo", e);
    }
  }

  void initGitRepo() {
    if (config.git == null) {
      return;
    }

    if (System.getenv("DEBUG") != null && System.getenv("DEBUG").equals("true")) {
      return;
    }
    try {
      if (new File(config.git.path).exists()) {
        FileUtils.forceDelete(new File(config.git.path));
      }
      this.gitRepo =
          Git.cloneRepository()
              .setURI(config.git.remote)
              .setDirectory(new File(config.git.path))
              .call();
    } catch (Exception e) {
      throw new RuntimeException("Can't init local shadowCache repo", e);
    }
  }
}
