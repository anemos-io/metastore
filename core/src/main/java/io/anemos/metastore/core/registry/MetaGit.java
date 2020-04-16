package io.anemos.metastore.core.registry;

import io.anemos.metastore.core.git.GitBase;
import io.anemos.metastore.core.git.GitConfig;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.RegistryP.SubmitSchemaRequest.Comment;
import io.opencensus.common.Scope;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaGit extends GitBase {
  private static final Logger LOG = LoggerFactory.getLogger(MetaGit.class);

  MetaGit(String name, GitConfig config) {
    super(name, config);
  }

  @Override
  protected void clean(ProtoDomain domain) throws GitAPIException {
    final List<String> descriptorFilesName =
        domain.getFileDescriptors().stream().map(m -> m.getFullName()).collect(Collectors.toList());

    clean(descriptorFilesName, new File(this.config.getPath()), ".proto");
  }

  @Override
  protected void sync(ProtoDomain protoContainer, Comment comment) {
    if (!config.isGitEnabled()) {
      return;
    }

    try (Scope scope = TRACER.spanBuilder("GitSync").setRecordEvents(true).startScopedSpan()) {
      if (System.getenv("DEBUG") != null && System.getenv("DEBUG").equals("true")) {
        protoContainer.writeToDirectory(new File(config.getPath()).toPath().toString());
        return;
      }

      pull();
      protoContainer.writeToDirectory(new File(config.getPath()).toPath().toString());
      gitRepo.add().addFilepattern(".").call();
      clean(protoContainer);
      Status status = gitRepo.status().call();
      if (status.hasUncommittedChanges()) {
        this.commit(comment);
        this.push();
        LOG.info("Git changes pushed");
      } else {
        LOG.info("No changes to commit");
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed syncing the git repo", e);
    }
  }
}
