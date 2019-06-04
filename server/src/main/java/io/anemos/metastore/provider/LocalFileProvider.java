package io.anemos.metastore.provider;

import com.google.protobuf.ByteString;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class LocalFileProvider implements StorageProvider {

  private String path;

  public LocalFileProvider(Map<String, String> config) {
    this.path = config.get("path");
    if (path == null) {
      throw new RuntimeException("path variable not set");
    }
    if (!new File(path).isDirectory()) {
      throw new RuntimeException(path + " is not a directory");
    }
    File file = new File(path);
    path = file.getPath();
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
