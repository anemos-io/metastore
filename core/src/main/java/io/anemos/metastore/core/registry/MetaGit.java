package io.anemos.metastore.core.registry;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import io.anemos.metastore.config.GitGlobalConfig;
import io.anemos.metastore.config.GitHostConfig;
import io.anemos.metastore.config.RegistryConfig;
import io.anemos.metastore.core.proto.PContainer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.util.FS;

class MetaGit {
  private static final Logger LOG = Logger.getLogger(MetaGit.class.getName());

  private Git gitRepo;
  private final RegistryConfig config;
  private final GitGlobalConfig global;

  MetaGit(RegistryConfig config, GitGlobalConfig global) {
    this.config = config;
    this.global = global;
  }

  void sync(PContainer protoContainer, String message) {
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
        gitRepo.commit().setMessage(message).call();
        gitRepo.push().call();
        LOG.info("shadowCache apply");
      } else {
        LOG.info("no changes to commit");
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed syncing the shadowCache repo", e);
    }
  }

  private File sshPrivateKey() throws IOException {
    String privateKeyBase64 = config.git.privateKey;
    if (privateKeyBase64 == null) {
      privateKeyBase64 = global != null ? global.privateKey : null;
    }
    if (privateKeyBase64 != null) {
      File ssh = File.createTempFile(config.name, "ssh");
      try (FileOutputStream outputStream = new FileOutputStream(ssh)) {
        outputStream.write(Base64.getDecoder().decode(privateKeyBase64));
      }
      return ssh;
    }
    return null;
  }

  public void init() {
    if (config.git == null) {
      return;
    }

    try {
      final File ssh = sshPrivateKey();

      if (System.getenv("DEBUG") != null && System.getenv("DEBUG").equals("true")) {
        return;
      }
      try {
        if (new File(config.git.path).exists()) {
          FileUtils.forceDelete(new File(config.git.path));
        }

        SshSessionFactory sshSessionFactory =
            new JschConfigSessionFactory() {
              @Override
              protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch(fs);
                defaultJSch.setConfig("HashKnownHosts", "yes");
                defaultJSch.addIdentity(ssh.getPath());
                if(global.hosts != null) {
                    for (GitHostConfig host : global.hosts) {
                        defaultJSch.getHostKeyRepository().add(
                                new HostKey(host.host, HostKey.ECDSA256, Base64.getDecoder().decode(host.key)),
                                null);
                    }
                }
                return defaultJSch;
              }

              @Override
              protected void configure(OpenSshConfig.Host host, Session session) {
                System.out.println(host);
                session.setUserInfo(new UserInfo() {
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
                    public void showMessage(String message) {

                    }
                });
                // do nothing
              }
            };

        this.gitRepo =
            Git.cloneRepository()
                .setURI(config.git.remote)
                .setDirectory(new File(config.git.path))
                .setTransportConfigCallback(
                    new TransportConfigCallback() {
                      @Override
                      public void configure(Transport transport) {
                        SshTransport sshTransport = (SshTransport) transport;
                        sshTransport.setSshSessionFactory(sshSessionFactory);
                      }
                    })
                .call();
      } catch (Exception e) {
        throw new RuntimeException("Can't init local shadowCache repo", e);
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
