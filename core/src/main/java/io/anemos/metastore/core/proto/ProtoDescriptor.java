package io.anemos.metastore.core.proto;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ProtoDescriptor {

    private Map<String, Descriptors.FileDescriptor> fileDescriptorMap;
    private Map<String, Descriptors.Descriptor> descriptorMap;

    public ProtoDescriptor(String file) throws IOException {
        this(new File(file));
    }

    public ProtoDescriptor(File file) throws IOException {
        this(new FileInputStream(file));
    }

    public ProtoDescriptor() {
        DescriptorProtos.FileDescriptorSet fileDescriptorProto = DescriptorProtos.FileDescriptorSet.newBuilder().build();
        fileDescriptorMap = Convert.convertFileDescriptorSet(fileDescriptorProto);
        indexDescriptorByName();
    }

    public ProtoDescriptor(InputStream inputStream) throws IOException {
        DescriptorProtos.FileDescriptorSet fileDescriptorProto = DescriptorProtos.FileDescriptorSet.parseFrom(inputStream);
        fileDescriptorMap = Convert.convertFileDescriptorSet(fileDescriptorProto);
        indexDescriptorByName();
    }

    public ProtoDescriptor(byte[] buffer) throws IOException {
        DescriptorProtos.FileDescriptorSet fileDescriptorProto = DescriptorProtos.FileDescriptorSet.parseFrom(buffer);
        fileDescriptorMap = Convert.convertFileDescriptorSet(fileDescriptorProto);
        indexDescriptorByName();
    }

    public ProtoDescriptor(Descriptors.Descriptor descriptor) throws IOException {
        fileDescriptorMap = new HashMap<>();
        Descriptors.FileDescriptor fileDescriptor = descriptor.getFile();
        fileDescriptorMap.put(fileDescriptor.getFullName(), fileDescriptor);
        indexDescriptorByName();
    }

    public Descriptors.FileDescriptor getFileDescriptorByFileName(String fileName) {
        return fileDescriptorMap.get(fileName);
    }

    public List<Descriptors.FileDescriptor> getFileDescriptorsByPackagePrefix(String packagePrefix) {
        return fileDescriptorMap.values().stream().filter(fd -> fd.getPackage().startsWith(packagePrefix))
                .collect(Collectors.toList());
    }

    public Set<String> getFileNames() {
        return fileDescriptorMap.keySet();
    }

    public List<Descriptors.FileDescriptor> getFileDescriptors() {
        return fileDescriptorMap.values()
                .stream().collect(Collectors.toList());
    }

    public void writeToDirectory(String root) throws IOException {
        for (Map.Entry<String, Descriptors.FileDescriptor> entry : fileDescriptorMap.entrySet()) {
            File file = new File(root + entry.getKey());
            file.getParentFile().mkdirs();
            try (OutputStream out = new FileOutputStream(file)) {
                ProtoLanguageFileWriter.write(entry.getValue(), out);
            }
        }
    }


    private void indexNestedDescriptorByName(List<Descriptors.Descriptor> nestedTypes) {
        nestedTypes.forEach(mt -> {
            descriptorMap.put(mt.getFullName(), mt);
            indexNestedDescriptorByName(mt.getNestedTypes());
        });
    }

    private void indexDescriptorByName() {
        descriptorMap = new HashMap<>();
        fileDescriptorMap.forEach(
                (k, v) -> {
                    v.getMessageTypes().forEach(
                            mt -> {
                                descriptorMap.put(mt.getFullName(), mt);
                                indexNestedDescriptorByName(mt.getNestedTypes());
                            }
                    );
                });
    }

    public Descriptors.Descriptor getDescriptorByName(String messageName) {
        return descriptorMap.get(messageName);
    }

    public byte[] toByteArray() {
        DescriptorProtos.FileDescriptorSet.Builder builder = DescriptorProtos.FileDescriptorSet.newBuilder();
        fileDescriptorMap.forEach((name, fd) -> builder.addFile(fd.toProto()));
        return builder.build().toByteArray();
    }

    public ByteString toByteString() {
        DescriptorProtos.FileDescriptorSet.Builder builder = DescriptorProtos.FileDescriptorSet.newBuilder();
        fileDescriptorMap.forEach((name, fd) -> builder.addFile(fd.toProto()));
        return builder.build().toByteString();
    }
}
