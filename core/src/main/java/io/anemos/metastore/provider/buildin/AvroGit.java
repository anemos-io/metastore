package io.anemos.metastore.provider.buildin;

import com.google.protobuf.Descriptors;
import io.anemos.metastore.core.git.GitBase;
import io.anemos.metastore.core.git.GitConfig;
import io.anemos.metastore.core.proto.ProtoToAvroSchema;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.RegistryP;
import io.opencensus.common.Scope;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvroGit extends GitBase {
  private static final Logger LOG = LoggerFactory.getLogger(AvroGit.class);
  private String rootOptionName;
  private List<String> schemaFiles;

  private AvroGit(String name, GitConfig config, String rootOptionName) {
    super(name, config);
    this.rootOptionName = rootOptionName;
  }

  public static void write(
      String name, GitConfig config, ProtoDomain protoDomain, String rootOptionName)
      throws IOException {

    RegistryP.SubmitSchemaRequest.Comment comment =
        RegistryP.SubmitSchemaRequest.Comment.newBuilder()
            .setDescription("Sync avro schema files.")
            .build();
    AvroGit gitWriteOnly = new AvroGit(name, config, rootOptionName);
    gitWriteOnly.init();
    gitWriteOnly.sync(protoDomain, comment);
  }

  @Override
  protected void sync(ProtoDomain protoContainer, RegistryP.SubmitSchemaRequest.Comment comment)
      throws IOException {
    if (!config.isGitEnabled()) {
      return;
    }
    final Collection<Descriptors.Descriptor> descriptors =
        protoContainer.findDescriptorsByOption(rootOptionName);

    schemaFiles = new ArrayList<>();
    for (Descriptors.Descriptor descriptor : descriptors) {
      final String fileName = descriptor.getFullName();
      final String fullName = String.format("%s.avsc", fileName.replace(".", "/"));
      final String avroSchema = ProtoToAvroSchema.convert(protoContainer, fileName);
      schemaFiles.add(fullName);
      writeToFile(avroSchema, fullName, new File(config.getPath()).toPath().toString());
    }

    try (Scope scope = TRACER.spanBuilder("GitSync").setRecordEvents(true).startScopedSpan()) {
      if (System.getenv("DEBUG") != null && System.getenv("DEBUG").equals("true")) {
        return;
      }

      pull();
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

  @Override
  protected void clean(ProtoDomain domain) throws GitAPIException {
    clean(schemaFiles, new File(this.config.getPath()), ".avsc");
  }

  private void writeToFile(String avroSchema, String fileName, String config) throws IOException {
    File file = new File(String.format("%s/%s", config, fileName));
    file.getParentFile().mkdirs();
    try (OutputStream out = new FileOutputStream(file)) {

      PrintWriter printWriter = new PrintWriter(out);
      printWriter.print(avroSchema);
      printWriter.flush();
    }
  }
}
