package io.anemos.metastore.provider;

import com.google.protobuf.ByteString;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class LocalFileStorage implements StorageProvider {

  private String path;

  public LocalFileStorage(Map<String, String> config) throws IOException {
    this.path = config.get("path");
    if (path == null) {
      throw new RuntimeException("path variable not set");
    }
    if (!new File(path).isDirectory()) {
      Files.createDirectories(new File(path).toPath(), new FileAttribute[] {});
    }
  }

  @Override
  public ByteString read(String fileName) {
    String filePath = path + "/" + fileName;
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
  public void write(String fileName, ByteString payload) {
    String filePath = path + "/" + fileName;
    try {
      FileUtils.writeByteArrayToFile(new File(filePath), payload.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException("failed write to " + filePath, e);
    }
  }
}
