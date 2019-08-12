package io.anemos.metastore.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.io.LineReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.input.CharSequenceReader;

class BindDatabase {

  private Map<String, BindResult> data = new HashMap<>();

  public void bindMessage(String resourceUrn, String messageName) {
    data.put(resourceUrn, new BindResult(resourceUrn, messageName, null));
  }

  public void bindService(String resourceUrn, String serviceName) {
    data.put(resourceUrn, new BindResult(resourceUrn, null, serviceName));
  }

  public void read(Reader reader) throws IOException {
    ObjectMapper om = new ObjectMapper();
    LineReader lineReader = new LineReader(reader);
    String line;
    while ((line = lineReader.readLine()) != null) {
      JsonLine jsonLine = om.readValue(line, JsonLine.class);
      data.put(
          jsonLine.linkedResource,
          new BindResult(jsonLine.linkedResource, jsonLine.messageName, jsonLine.serviceName));
    }
  }

  void write(Writer fileWriter) throws IOException {
    ObjectWriter writer = new ObjectMapper().writerWithType(JsonLine.class);
    data.forEach(
        (key, value) -> {
          JsonLine jl = new JsonLine();
          jl.linkedResource = key;
          jl.messageName = value.getMessageName();
          jl.serviceName = value.getServiceName();
          try {
            fileWriter.write(writer.writeValueAsString(jl));
            fileWriter.write('\n');
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
    fileWriter.flush();
  }

  BindResult get(String resourceUrn) {
    return data.get(resourceUrn);
  }

  List<BindResult> list(String nextPageToken) {
    List<BindResult> results = new ArrayList<>();
    data.forEach(
        (k, v) -> {
          results.add(v);
        });
    return results;
  }

  BindResult remove(String resourceUrn) {
    return data.remove(resourceUrn);
  }

  byte[] toByteArray() {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      write(new OutputStreamWriter(byteArrayOutputStream));
    } catch (IOException e) {
      return null;
    }
    return byteArrayOutputStream.toByteArray();
  }

  public void parse(byte[] buffer) throws IOException {
    read(new CharSequenceReader(new String(buffer)));
  }

  private static class JsonLine {
    public String linkedResource;
    public String messageName;
    public String serviceName;
  }
}
