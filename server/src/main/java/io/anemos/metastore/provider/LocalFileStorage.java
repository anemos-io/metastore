package io.anemos.metastore.provider;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class LocalFileStorage implements StorageProvider, BindProvider {

  private String path;
  private BindDatabase bindDatabase;
  private String registyName;

  private void init(RegistryInfo registryInfo, Map<String, String> config) {
    this.registyName = registryInfo.getName();
    this.path = config.get("path");
    if (path == null) {
      throw new RuntimeException("path variable not set");
    }
    if (!new File(path).isDirectory()) {
      try {
        Files.createDirectories(new File(path).toPath(), new FileAttribute[] {});
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void initForBind(RegistryInfo registryInfo, Map<String, String> config, boolean readOnly) {
    init(registryInfo, config);
    loadBind();
  }

  @Override
  public void initForStorage(RegistryInfo registryInfo, Map<String, String> config) {
    init(registryInfo, config);
  }

  @Override
  public ByteString read() {
    String filePath = path + "/" + registyName + ".pb";
    try {
      if (!new File(filePath).exists()) {
        return null;
      }
      return ByteString.copyFrom(IOUtils.toByteArray(new FileInputStream(filePath)));
    } catch (IOException e) {
      throw new RuntimeException("failed to read " + filePath, e);
    }
  }

  @Override
  public void write(ByteString payload) {
    String filePath = path + "/" + registyName + ".pb";
    try {
      FileUtils.writeByteArrayToFile(new File(filePath), payload.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException("failed write to " + filePath, e);
    }
  }

  @Override
  public void createResourceBinding(String resourceUrn, Descriptors.Descriptor descriptor) {
    bindDatabase.bindMessage(resourceUrn, descriptor.getFullName());
    saveBind();
  }

  @Override
  public void updateResourceBinding(String resourceUrn, Descriptors.Descriptor descriptor) {
    bindDatabase.bindMessage(resourceUrn, descriptor.getFullName());
    saveBind();
  }

  @Override
  public void createServiceBinding(String resourceUrn, Descriptors.ServiceDescriptor descriptor) {
    bindDatabase.bindService(resourceUrn, descriptor.getFullName());
    saveBind();
  }

  @Override
  public void updateServiceBinding(String resourceUrn, Descriptors.ServiceDescriptor descriptor) {
    bindDatabase.bindService(resourceUrn, descriptor.getFullName());
    saveBind();
  }

  @Override
  public void deleteResourceBinding(String resourceUrn) {
    bindDatabase.remove(resourceUrn);
    saveBind();
  }

  @Override
  public List<BindResult> listResourceBindings(String next_page_token) {
    return bindDatabase.list(next_page_token);
  }

  @Override
  public boolean isWriteOnly() {
    return false;
  }

  @Override
  public BindResult getResourceBinding(String resourceUrn) {
    return bindDatabase.get(resourceUrn);
  }

  private void saveBind() {
    String filePath = path + "/" + registyName + ".bind";
    try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filePath))) {
      bindDatabase.write(writer);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void loadBind() {
    String filePath = path + "/" + registyName + ".bind";
    File file = new File(filePath);
    bindDatabase = new BindDatabase();
    if (file.exists()) {
      try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file))) {
        bindDatabase.read(reader);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
