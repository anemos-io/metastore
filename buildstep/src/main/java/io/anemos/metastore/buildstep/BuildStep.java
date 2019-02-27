package io.anemos.metastore.buildstep;

import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ProtoLint;
import io.anemos.metastore.core.proto.validate.ValidationResults;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BuildStep {


//    public static void main(String... args) {
//        File[] files = new File("C:/").listFiles();
//        showFiles(files);
//    }

    public static void showFiles(File[] files, PrintWriter writer) throws IOException {
        for (File file : files) {
            if (file.isDirectory()) {
                showFiles(file.listFiles(), writer); // Calls same method again.
            } else {
                if (file.getName().toLowerCase().endsWith(".proto")) {
                    writer.println(file.getCanonicalPath());
                }
            }
        }
    }


    public static File listProtos(File workspace) throws IOException {
        File f = File.createTempFile("protofiles", ".txt");
        f.deleteOnExit();
        OutputStream outputStream = new FileOutputStream(f);
        PrintWriter printWriter = new PrintWriter(outputStream);

        showFiles(workspace.listFiles(), printWriter);
        printWriter.flush();
        printWriter.close();
        return f;
    }

    public static void main(String... args) throws IOException {
        //File workspace = new File("/Users/AlexVB/Repos/src/github.com/googleapis/googleapis");
        File workspace = new File("testsets/play");

        File file = listProtos(workspace);

        try {
            List<String> command = new ArrayList<>();
            command.add("protoc");
            if (true) {
                command.add("-Itmp/include");
                command.add("-I" + workspace.getCanonicalPath());
            }
            command.add("--descriptor_set_out=tmp/descriptor.pb");
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

        ProtoDescriptor container = new ProtoDescriptor("tmp/descriptor.pb");
        ProtoDescriptor reference = new ProtoDescriptor("tmp/test1.pb");


        ValidationResults results = new ValidationResults();

        ProtoLint lint = new ProtoLint(
                container,
                results
        );
        ProtoDiff diff = new ProtoDiff(
                reference,
                container,
                results
        );
        //lint.lintOnMessage(null);

        lint.lint();
        diff.diffOnFileName("test/v1alpha1/simple.proto");
//        lint.lintOnFileName("google/monitoring/v3/notification_service.proto");

        System.out.print(results.getResult());

    }

    private static int protoc(List<String> command) throws IOException {
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
        return p.exitValue();
    }
}
