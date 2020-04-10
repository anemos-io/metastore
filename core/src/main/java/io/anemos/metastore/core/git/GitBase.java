package io.anemos.metastore.core.git;

import com.jcraft.jsch.*;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.RegistryP;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GitBase {
  protected static final Logger LOG = LoggerFactory.getLogger(GitBase.class);
  protected static final Tracer TRACER = Tracing.getTracer();
  protected GitConfig config;
  private String name;
  protected Git gitRepo;
  protected TransportConfigCallback transportConfigCallback;

  public GitBase(String name, GitConfig config) {
    this.name = name;
    this.config = config;
  }

  public void init() {
    if (!config.isGitEnabled()) {
      return;
    }
    LOG.info("Git Enabled");

    try {
      final File ssh = sshPrivateKey();

      if (System.getenv("DEBUG") != null && System.getenv("DEBUG").equals("true")) {
        return;
      }
      try {
        if (new File(config.getPath()).exists()) {
          FileUtils.forceDelete(new File(config.getPath()));
        }

        JschConfigSessionFactory sshSessionFactory =
            new JschConfigSessionFactory() {
              @Override
              protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch(fs);
                defaultJSch.setConfig("HashKnownHosts", "yes");
                defaultJSch.setConfig("StrictHostKeyChecking", "no");
                defaultJSch.addIdentity(ssh.getPath());
                if (config.hasHosts()) {
                  HostKeyRepository hostKeyRepository = defaultJSch.getHostKeyRepository();
                  for (HostKey hostKey : config.getHostKeys()) {
                    hostKeyRepository.add(hostKey, null);
                  }
                }
                return defaultJSch;
              }

              @Override
              protected void configure(OpenSshConfig.Host host, Session session) {
                session.setUserInfo(
                    new UserInfo() {
                      @Override
                      public String getPassphrase() {
                        return null;
                      }

                      @Override
                      public String getPassword() {
                        return null;
                      }

                      @Override
                      public boolean promptPassword(String message) {
                        return false;
                      }

                      @Override
                      public boolean promptPassphrase(String message) {
                        return false;
                      }

                      @Override
                      public boolean promptYesNo(String message) {
                        return true;
                      }

                      @Override
                      public void showMessage(String message) {}
                    });
                // do nothing
              }
            };
        transportConfigCallback =
            new TransportConfigCallback() {
              @Override
              public void configure(Transport transport) {
                SshTransport sshTransport = (SshTransport) transport;
                sshTransport.setSshSessionFactory(sshSessionFactory);
              }
            };

        LOG.info("Git Local: " + config.getPath());
        LOG.info("Git Remote: " + config.getRemote());
        this.gitRepo =
            Git.cloneRepository()
                .setURI(config.getRemote())
                .setDirectory(new File(config.getPath()))
                .setTransportConfigCallback(transportConfigCallback)
                .call();
      } catch (Exception e) {
        throw new RuntimeException("Can't init local shadowCache repo", e);
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void push() throws GitAPIException {
    gitRepo.push().setTransportConfigCallback(transportConfigCallback).call();
  }

  protected void pull() throws GitAPIException {
    gitRepo.pull().setTransportConfigCallback(transportConfigCallback).call();
  }

  protected void commit(RegistryP.SubmitSchemaRequest.Comment comment) throws GitAPIException {
    CommitCommand commit = gitRepo.commit();
    if (comment.getDescription().length() > 0) {
      commit.setMessage(comment.getDescription());
    } else {
      commit.setMessage("No message provider");
    }
    if (comment.getEmail().length() > 0 || comment.getName().length() > 0) {
      commit.setAuthor(comment.getName(), comment.getEmail());
    }
    commit.call();
  }

  protected File sshPrivateKey() throws IOException {
    String privateKeyBase64 = config.getPrivateKeyBase64();
    if (privateKeyBase64 != null) {
      File ssh = File.createTempFile(name, "ssh");
      try (FileOutputStream outputStream = new FileOutputStream(ssh)) {
        outputStream.write(Base64.getDecoder().decode(privateKeyBase64));
      }
      return ssh;
    }
    return null;
  }

  protected abstract void sync(
      ProtoDomain protoContainer, RegistryP.SubmitSchemaRequest.Comment comment) throws IOException;

  protected abstract void clean(ProtoDomain domain) throws GitAPIException;

  protected void clean(List<String> descriptorFilesName, File dir, String fileExtension)
      throws GitAPIException {
    File repo = new File(config.getPath());
    File[] files = dir.listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        clean(descriptorFilesName, file, fileExtension);
      } else if (file.isFile() && file.getName().toLowerCase().endsWith(fileExtension)) {
        String filePattern = file.getAbsolutePath().replace(repo.getAbsolutePath(), "");
        if (filePattern.startsWith("/")) {
          filePattern = filePattern.substring(1);
        }
        if (!descriptorFilesName.contains(filePattern)) {
          gitRepo.rm().addFilepattern(filePattern).call();
        }
      }
    }
  }
}
