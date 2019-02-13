package io.anemos.metastore.buildstep;

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
        System.out.println("Success");

        File workspace = new File("/Users/AlexVB/Repos/src/github.com/googleapis/googleapis");

        File file = listProtos(workspace);

        try {

            /*
            protoc \
 -Iserver/src/main/proto \
 -I/usr/local/include \
 -I$GOOGLEAPIS_DIR \
 --descriptor_set_out=tmp/service_descriptor.pb \
 --include_imports \
 --include_source_info \
 server/src/main/proto/io/anemos/metastore/metastore.proto \
 server/src/main/proto/io/anemos/metastore/schemaregistry.proto
             */

            List<String> command = new ArrayList<>();
            command.add("protoc");
            command.add("-I/usr/local/include");
            command.add("-I"+workspace.getCanonicalPath());
            command.add("--descriptor_set_out=tmp/descriptor.pb");
            command.add("--include_imports");
            command.add("--include_source_info");
            command.add("@" + file.getCanonicalPath());

            ProcessBuilder builder = new ProcessBuilder(command);

;
            Process p = builder.start();

            // enter code here

            try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;

                while ((line = input.readLine()) != null) {
                    System.out.println(line);
                }
            }

        } catch (Exception err) {
            err.printStackTrace();
        }

        System.exit(0);
    }
}
