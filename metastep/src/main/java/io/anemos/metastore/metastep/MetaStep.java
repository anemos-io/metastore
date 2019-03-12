package io.anemos.metastore.metastep;

import com.google.protobuf.ByteString;
import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.v1alpha1.Report;
import io.anemos.metastore.v1alpha1.ResultCount;
import io.anemos.metastore.v1alpha1.SchemaRegistyServiceGrpc;
import io.anemos.metastore.v1alpha1.Schemaregistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class MetaStep {

    File workspace;
    String protoInclude;
    SchemaRegistyServiceGrpc.SchemaRegistyServiceBlockingStub schemaRegistry;
    private boolean descriptorIsTemp = false;
    private File descriptorFile;
    private Namespace res;

    public MetaStep(String... args) throws IOException, ArgumentParserException {
        ArgumentParser parser = ArgumentParsers.newFor("metastrep")
                .build();

        Subparsers subparsers = parser.addSubparsers().help("sub-command help");

        Subparser submitParser = subparsers.addParser("publish").help("publish help");
        submitParser.setDefault("sub-command", "publish");
        submitParser.addArgument("-p", "--package_prefix").required(true);
        submitParser.addArgument("-d", "--descriptor_set").required(false);
        submitParser.addArgument("-w", "--workspace").required(false);
        submitParser.addArgument("-s", "--server").required(true);

        Subparser validateParser = subparsers.addParser("validate").help("validate help");
        validateParser.setDefault("sub-command", "validate");
        validateParser.addArgument("-p", "--package_prefix").required(true);
        validateParser.addArgument("-d", "--descriptor_set").required(false);
        validateParser.addArgument("-f", "--profile").required(false);
        validateParser.addArgument("-w", "--workspace").required(false);
        validateParser.addArgument("-s", "--server").required(true);


//        streamParser.addArgument("-e", "--env")
//                .choices("production", "staging", "integration")
//                .required(true);
//
        res = parser.parseArgs(args);


        descriptorFile = File.createTempFile("descriptor", ".pb");
        descriptorIsTemp = true;

        String server = res.getString("server");
        String[] sp = server.split(":");
        String host = sp[0];
        Integer port = Integer.parseInt(sp[1]);

        String protoWorkspace = res.getString("workspace");
        if (protoWorkspace == null) {
            protoWorkspace = ".";
        }
        workspace = new File(protoWorkspace);

        protoInclude = System.getenv("PROTO_INCLUDE");
        if (protoInclude == null) {
            protoInclude = "/usr/include";
        }


        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        schemaRegistry = SchemaRegistyServiceGrpc.newBlockingStub(channel);


//        Schemaregistry.GetSchemaResponse response = schemaRegistry
//                .getSchema(Schemaregistry.GetSchemaRequest.newBuilder().build());

        //File workspace = new File("/Users/AlexVB/Repos/src/github.com/googleapis/googleapis");
    }

    public static void main(String... args) throws IOException, ArgumentParserException {

        System.out.println("MetaStep");
        if (args[0].equals("sh")) {
            GitLabMagic gitLabMagic = new GitLabMagic();

            if (gitLabMagic.gitLabEmptyCall) {
                System.out.println("Empty call ignored.");
                System.exit(0);
            }

            if (gitLabMagic.gitLabArgs == null) {
                System.err.println("! No arguments detected.");
                System.exit(1);
            } else {
                System.out.println("- Detected args: " + Arrays.asList(gitLabMagic.gitLabArgs));
            }

            if (gitLabMagic.workDir == null) {
                System.err.println("! No workdir detected.");
                System.exit(1);
            } else {
                System.out.println("- Detected workdir: " + gitLabMagic.workDir);
            }

            args = Stream.concat(
                    Arrays.stream(gitLabMagic.gitLabArgs),
                    Arrays.stream(new String[]{"--workspace", gitLabMagic.workDir}))
                    .toArray(String[]::new);
        }


        //System.exit(0);

        MetaStep metaStep = new MetaStep(args);
        metaStep.start();
        System.err.println();
        System.err.println();
    }

    private static void iterateProtoFiles(File[] files, PrintWriter writer) throws IOException {
        for (File file : files) {
            if (file.isDirectory()) {
                iterateProtoFiles(file.listFiles(), writer); // Calls same method again.
            } else {
                if (file.getName().toLowerCase().endsWith(".proto")) {
                    System.out.println(file.getCanonicalPath());
                    writer.println(file.getCanonicalPath());
                }
            }
        }
    }

    private static File listProtos(File workspace) throws IOException {
        File f = File.createTempFile("protofiles", ".txt");
        f.deleteOnExit();
        OutputStream outputStream = new FileOutputStream(f);
        PrintWriter printWriter = new PrintWriter(outputStream);

        iterateProtoFiles(workspace.listFiles(), printWriter);
        printWriter.flush();
        printWriter.close();
        return f;
    }

    private static int protoc(List<String> command) throws IOException, InterruptedException {
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

    public void start() throws IOException {
        switch (res.getString("sub-command")) {
            case "validate":
                validate();
                break;
            case "publish":
                publish();
                break;
            default:
                System.out.println(res);

        }
    }

    private ProtoDescriptor createDescriptorSet() throws IOException {
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

        return new ProtoDescriptor(descriptorFile);
    }

    private void validate() throws IOException {
        System.out.println("Contract Validation started");
        ProtoDescriptor protoContainer = createDescriptorSet();

        Schemaregistry.SubmitSchemaResponse verifySchemaResponse = schemaRegistry.verifySchema(Schemaregistry.SubmitSchemaRequest.newBuilder()
                .setFdProtoSet(protoContainer.toByteString())
                .addScope(Schemaregistry.Scope.newBuilder()
                        .setPackagePrefix(res.getString("package_prefix"))
                        .build())
                .build());

        Report report = verifySchemaResponse.getReport();

        ResultCount resultCount = report.getResultCount();
        int errors = 0, warnings = 0, infos = 0;
        errors += resultCount.getDiffErrors();
        errors += resultCount.getLintErrors();
        warnings += resultCount.getLintWarnings();
        warnings += resultCount.getDiffWarnings();
        System.out.print(report);

        System.out.println(String.format("%d errors, %d warnings and %d infos", errors, warnings, infos));
        if (errors > 0) {
            System.exit(1);
        }

    }

    private void publish() throws IOException {
        System.out.println("Contract Push started");
        ProtoDescriptor protoContainer = createDescriptorSet();
        schemaRegistry.submitSchema(Schemaregistry.SubmitSchemaRequest.newBuilder()
                .setFdProtoSet(ByteString.copyFrom(protoContainer.toByteArray()))
                .addScope(Schemaregistry.Scope.newBuilder()
                        .setPackagePrefix(res.getString("package_prefix"))
                        .build())
                .build());

    }
}
