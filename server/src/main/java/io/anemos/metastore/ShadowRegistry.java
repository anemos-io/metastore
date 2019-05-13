package io.anemos.metastore;

import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.core.proto.shadow.ShadowApply;
import io.anemos.metastore.v1alpha1.Report;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;

public class ShadowRegistry {

  private static final Logger LOG = Logger.getLogger(ShadowRegistry.class.getName());

  private MetaStore metaStore;
  private ProtoDescriptor shadow;
  private Report delta;
  private Git shadowRepo;
  private Path shadowRepoPath;

  public ShadowRegistry(MetaStore metaStore, Report delta) {
    this.metaStore = metaStore;
    this.delta = delta;
    if (System.getenv("METASTORE_SHADOW_REPO_PATH") != null) {
      this.shadowRepoPath = new File(System.getenv("METASTORE_SHADOW_REPO_PATH")).toPath();
    } else {
      try {
        this.shadowRepoPath = Files.createTempDirectory("");
      } catch (IOException e) {
        throw new RuntimeException("Failed to create temp directory");
      }
    }
    this.shadow = getShadow();
    initRepo();
    sync();
  }

  private void initRepo() {
    if (System.getenv("DEBUG") != null && System.getenv("DEBUG").equals("true")) {
      return;
    }
    try {
      if (shadowRepoPath.toFile().exists()) {
        FileUtils.forceDelete(shadowRepoPath.toFile());
      }
      this.shadowRepo =
          Git.cloneRepository()
              .setURI(System.getenv("METASTORE_SHADOW_REPO_URI"))
              .setDirectory(shadowRepoPath.toFile())
              .call();
    } catch (Exception e) {
      throw new RuntimeException("Can't init local shadow repo", e);
    }
  }

  public void sync() {
    try {
      if (System.getenv("DEBUG") != null && System.getenv("DEBUG").equals("true")) {
        getShadow().writeToDirectory(shadowRepoPath.toString());
        return;
      }

      shadowRepo.pull();
      getShadow().writeToDirectory(shadowRepoPath.toString());
      shadowRepo.add().addFilepattern(".").call();
      Status status = shadowRepo.status().call();
      if (status.hasUncommittedChanges()) {
        shadowRepo.commit().setMessage("shadow apply").call();
        shadowRepo.push().call();
        LOG.info("shadow apply");
      } else {
        LOG.info("no changes to commit");
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed syncing the shadow repo", e);
    }
  }

  public Report getDelta() {
    return this.delta;
  }

  public ProtoDescriptor getShadow() {
    metaStore.readDefault();
    this.shadow = new ShadowApply().applyDelta(metaStore.repo, this.delta);
    shadow.registerOptions();
    return shadow;
  }

  public void setDelta(Report delta) {
    this.delta = delta;
  }
}
