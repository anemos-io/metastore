package io.anemos.metastore.core.git;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSchException;
import io.anemos.metastore.config.GitGlobalConfig;
import io.anemos.metastore.config.GitProviderConfig;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GitConfig {

  private boolean hasGit = false;
  private String path;
  private String remote;
  private String privateKeyBase64;
  private HostKey[] hostKeys;

  public static final String KEY_PATH = "git.path";
  public static final String KEY_REMOTE = "git.remote";
  public static final String KEY_PRIVATE_KEY = "git.private_key";
  public static final String KEY_HOSTS_COUNT = "git.hosts.count";
  public static final String KEY_HOSTS_PREFIX = "git.hosts.";
  public static final String KEY_HOSTS_HOST_SUFFIX = ".host";
  public static final String KEY_HOSTS_TYPE_SUFFIX = ".type";
  public static final String KEY_HOSTS_KEY_SUFFIX = ".key";

  public static GitConfig fromConfigFile(GitProviderConfig config, GitGlobalConfig global) {
    GitConfig gitConfig = new GitConfig();
    if (config == null) {
      gitConfig.hasGit = false;
      return gitConfig;
    }
    gitConfig.hasGit = true;
    gitConfig.path = config.path;
    if (gitConfig.path == null) {
      throw new RuntimeException("git path needs to be set when git is enabled.");
    }
    gitConfig.remote = config.remote;
    if (gitConfig.remote == null) {
      throw new RuntimeException("git remote needs to be set when git is enabled.");
    }
    gitConfig.privateKeyBase64 = config.privateKey;
    if (gitConfig.privateKeyBase64 == null) {
      gitConfig.privateKeyBase64 = global != null ? global.privateKey : null;
    }
    if (gitConfig.privateKeyBase64 == null) {
      throw new RuntimeException("git private key needs to be set when git is enabled.");
    }
    if (global != null && global.hosts != null) {
      gitConfig.hostKeys = new HostKey[global.hosts.length];
      for (int i = 0; i < global.hosts.length; i++) {
        try {
          gitConfig.hostKeys[i] =
              new HostKey(
                  global.hosts[i].host,
                  HostKey.ECDSA256,
                  Base64.getDecoder().decode(global.hosts[i].key),
                  null);
        } catch (JSchException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return gitConfig;
  }

  public static GitConfig fromMap(Map<String, String> config) {
    GitProviderConfig gitProviderConfig = new GitProviderConfig();
    gitProviderConfig.path = config.get(KEY_PATH);
    gitProviderConfig.remote = config.get(KEY_REMOTE);
    gitProviderConfig.privateKey = config.get(KEY_REMOTE);

    GitGlobalConfig gitGlobalConfig = new GitGlobalConfig();
    int count = Integer.parseInt(config.get(KEY_HOSTS_COUNT));
    gitGlobalConfig.hosts = new GitGlobalConfig.GitHostConfig[count];
    for (int i = 0; i < count; i++) {
      GitGlobalConfig.GitHostConfig hostConfig = new GitGlobalConfig.GitHostConfig();
      hostConfig.host =
          config.get(String.format("%s%d%s", KEY_HOSTS_PREFIX, i, KEY_HOSTS_HOST_SUFFIX));
      hostConfig.type =
          config.get(String.format("%s%d%s", KEY_HOSTS_PREFIX, i, KEY_HOSTS_TYPE_SUFFIX));
      hostConfig.key =
          config.get(String.format("%s%d%s", KEY_HOSTS_PREFIX, i, KEY_HOSTS_KEY_SUFFIX));
      gitGlobalConfig.hosts[i] = hostConfig;
    }
    return fromConfigFile(gitProviderConfig, gitGlobalConfig);
  }

  public Map<String, String> toMap() {
    Map<String, String> map = new HashMap<>();
    if (isGitEnabled()) {
      map.put(KEY_PATH, getPath());
      map.put(KEY_REMOTE, getRemote());
      map.put(KEY_PRIVATE_KEY, getPrivateKeyBase64());
      map.put(KEY_HOSTS_COUNT, String.valueOf(hostKeys.length));
      for (int i = 0; i < hostKeys.length; i++) {
        HostKey hostKey = hostKeys[i];
        map.put(
            String.format("%s%d%s", KEY_HOSTS_PREFIX, i, KEY_HOSTS_HOST_SUFFIX), hostKey.getHost());
        map.put(
            String.format("%s%d%s", KEY_HOSTS_PREFIX, i, KEY_HOSTS_TYPE_SUFFIX), hostKey.getType());
        map.put(
            String.format("%s%d%s", KEY_HOSTS_PREFIX, i, KEY_HOSTS_KEY_SUFFIX), hostKey.getKey());
      }
    }
    return map;
  }

  public boolean hasHosts() {
    return hostKeys != null;
  }

  public Iterable<HostKey> getHostKeys() {
    return Arrays.stream(hostKeys).collect(Collectors.toSet());
  }

  public boolean isGitEnabled() {
    return hasGit;
  }

  public String getPath() {
    return path;
  }

  public String getRemote() {
    return remote;
  }

  public String getPrivateKeyBase64() {
    return privateKeyBase64;
  }

  public static class Host {
    private String host;
    private String key;
    private String type;

    public Host(String host, String key, String type) {
      this.host = host;
      this.key = key;
      this.type = type;
    }

    public String getHostName() {
      return host;
    }

    public String getKey() {
      return key;
    }

    public int getKeyType() {
      if (type == null) {
        return HostKey.ECDSA256;
      }
      return HostKey.ECDSA256;
    }
  }
}
