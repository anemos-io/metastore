package io.anemos.metastore.core.proto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ProtocUtil {

  public static PContainer createDescriptorSet(String workDir) throws IOException {
    File descriptorFile = File.createTempFile("descriptor", ".pb");

    String protoInclude = System.getenv("PROTO_INCLUDE");
    if (protoInclude == null) {
      protoInclude = "/usr/include";
    }

    File workspace = new File(workDir);
    File file = listProtos(workspace);

    try {
      List<String> command = new ArrayList<>();
      command.add("protoc");
      if (true) {
        command.add("-I" + protoInclude);
        command.add("-I" + workspace.getCanonicalPath());
      }
      command.add("--descriptor_set_out=" + descriptorFile.getCanonicalPath());
      command.add("--include_imports");
      //            command.add("--include_source_info");
      command.add("@" + file.getCanonicalPath());

      int pcexit = protoc(command);
      if (pcexit > 0) {
        System.exit(pcexit);
      }
    } catch (Exception err) {
      err.printStackTrace();
    }
    return new PContainer(descriptorFile);
  }

  public static int protoc(List<String> command) throws IOException, InterruptedException {
    ProcessBuilder builder = new ProcessBuilder(command);
    Process p = builder.start();
    try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
      String line;
      while ((line = input.readLine()) != null) {
        System.out.println(line);
      }
    }

    try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
      String line;
      while ((line = input.readLine()) != null) {
        System.err.println(line);
      }
    }
    p.waitFor();
    return p.exitValue();
  }

  public static File listProtos(File workspace) throws IOException {
    File f = File.createTempFile("protofiles", ".txt");
    f.deleteOnExit();
    OutputStream outputStream = new FileOutputStream(f);
    PrintWriter printWriter = new PrintWriter(outputStream);

    iterateProtoFiles(workspace.listFiles(), printWriter);
    printWriter.flush();
    printWriter.close();
    return f;
  }

  public static void iterateProtoFiles(File[] files, PrintWriter writer) throws IOException {
    for (File file : files) {
      if (file.isDirectory()) {
        System.out.println("D: " + file.getCanonicalPath());
        iterateProtoFiles(file.listFiles(), writer); // Calls same method again.
      } else {
        if (file.getName().toLowerCase().endsWith(".proto")) {
          System.out.println("P: " + file.getCanonicalPath());
          writer.println(file.getCanonicalPath());
        }
      }
    }
  }
}
