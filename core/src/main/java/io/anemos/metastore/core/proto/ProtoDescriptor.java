package io.anemos.metastore.core.proto;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ProtoDescriptor {

    private Map<String, Descriptors.FileDescriptor> fileDescriptorMap;
    private Map<String, Descriptors.Descriptor> descriptorMap;
    private Map<String, Descriptors.ServiceDescriptor> serviceMap;
    private Map<String, Descriptors.EnumDescriptor> enumMap;
    private Map<Integer, Descriptors.FileDescriptor> fileOptionMap;
    private Map<Integer, Descriptors.FileDescriptor> messageOptionMap;
    private Map<Integer, Descriptors.FileDescriptor> fieldOptionMap;

    public ProtoDescriptor(DescriptorProtos.FileDescriptorSet fileDescriptorSet) {
        fileDescriptorMap = Convert.convertFileDescriptorSet(fileDescriptorSet);
        indexDescriptorByName();
        indexOptionsByNumber();
    }

    public ProtoDescriptor(String file) throws IOException {
        this(new File(file));
    }

    public ProtoDescriptor(File file) throws IOException {
        this(new FileInputStream(file));
    }

    public ProtoDescriptor() {
        this(DescriptorProtos.FileDescriptorSet.newBuilder().build());
    }

    public ProtoDescriptor(InputStream inputStream) throws IOException {
        this(DescriptorProtos.FileDescriptorSet.parseFrom(inputStream));
    }

    public ProtoDescriptor(byte[] buffer) throws IOException {
        this(DescriptorProtos.FileDescriptorSet.parseFrom(buffer));
    }

    public ProtoDescriptor(Descriptors.Descriptor descriptor) throws IOException {
        fileDescriptorMap = new HashMap<>();
        Descriptors.FileDescriptor fileDescriptor = descriptor.getFile();
        fileDescriptorMap.put(fileDescriptor.getFullName(), fileDescriptor);
        indexDescriptorByName();
        indexOptionsByNumber();
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
            String packageDir = entry.getValue().getPackage().replaceAll("\\.", "/");
            String fileName = entry.getValue().getName();
            if (fileName.contains("/")) {
                fileName = entry.getValue().getName().substring(entry.getValue().getName().lastIndexOf("/") + 1);
            }
            File file = new File(String.format("%s/%s/%s", root, packageDir, fileName));
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
                (k, v) -> v.getMessageTypes().forEach(
                        mt -> {
                            descriptorMap.put(mt.getFullName(), mt);
                            indexNestedDescriptorByName(mt.getNestedTypes());
                        }
                ));
        serviceMap = new HashMap<>();
        fileDescriptorMap.forEach(
                (k, v) -> v.getServices().forEach(
                        mt -> {
                            serviceMap.put(mt.getFullName(), mt);
                        }
                ));
        enumMap = new HashMap<>();
        fileDescriptorMap.forEach(
                (k, v) -> v.getEnumTypes().forEach(
                        mt -> {
                            enumMap.put(mt.getFullName(), mt);
                        }
                ));
    }

    public Descriptors.Descriptor getDescriptorByName(String messageName) {
        return descriptorMap.get(messageName);
    }

    public Descriptors.ServiceDescriptor getServiceDescriptorByName(String messageName) {
        return serviceMap.get(messageName);
    }

    public void registerOptions() {
        fileDescriptorMap = Convert.registerOptions(fileDescriptorMap);
        indexDescriptorByName();
        indexOptionsByNumber();
    }

    public Descriptors.EnumDescriptor getEnumDescriptorByName(String messageName) {
        return enumMap.get(messageName);
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

    private void indexOptionsByNumber() {
        fileOptionMap = new HashMap<>();
        messageOptionMap = new HashMap<>();
        fieldOptionMap = new HashMap<>();
        fileDescriptorMap.forEach(
                (fileName, fileDescriptor) -> {
                    fileDescriptor.getExtensions().forEach(extension -> {
                        switch (extension.toProto().getExtendee()) {
                            case ".google.protobuf.FileOptions":
                                fileOptionMap.put(extension.getNumber(), fileDescriptor);
                                break;
                            case ".google.protobuf.MessageOptions":
                                messageOptionMap.put(extension.getNumber(), fileDescriptor);
                                break;
                            case ".google.protobuf.FieldOptions":
                                fieldOptionMap.put(extension.getNumber(), fileDescriptor);
                                break;
                        }
                    });
                }
        );
    }

    public DescriptorProtos.FileDescriptorSet toFileDescriptorSet() {
        DescriptorProtos.FileDescriptorSet.Builder setBuilder = DescriptorProtos.FileDescriptorSet.newBuilder();
        fileDescriptorMap.forEach((name, fd) -> {
            DescriptorProtos.FileDescriptorProto fileDescriptorProto = DescriptorProtos.FileDescriptorProto.newBuilder(fd.toProto()).build();
            setBuilder.addFile(fileDescriptorProto);
        });
        return setBuilder.build();
    }

    public ProtoDescriptor update(Collection<DescriptorProtos.FileDescriptorProto> newFileDescriptorProtos) throws IOException {
        DescriptorProtos.FileDescriptorSet fileDescriptorSet = this.toFileDescriptorSet();
        Map<String, Integer> fileIndices = new HashMap<>();
        for (int i = 0; i < fileDescriptorSet.getFileCount(); i++) {
            fileIndices.put(fileDescriptorSet.getFile(i).getName(), i);
        }
        DescriptorProtos.FileDescriptorSet.Builder setBuilder = DescriptorProtos.FileDescriptorSet.newBuilder(fileDescriptorSet);

        for (DescriptorProtos.FileDescriptorProto newFileDescriptorProto : newFileDescriptorProtos) {

            //TODO add recursive options
            Set<Integer> fileOptionDependencyNumbers = new HashSet<>(newFileDescriptorProto.getOptions().getUnknownFields().asMap().keySet());
            Set<Integer> messageOptionDependencyNumbers = new HashSet<>();
            Set<Integer> fieldOptionDependencyNumbers = new HashSet<>();
            for (DescriptorProtos.DescriptorProto descriptorProto : newFileDescriptorProto.getMessageTypeList()) {
                messageOptionDependencyNumbers.addAll(descriptorProto.getUnknownFields().asMap().keySet());
                descriptorProto.getFieldList().forEach(field -> fieldOptionDependencyNumbers.addAll(field.getUnknownFields().asMap().keySet()));
            }

            ArrayList<Descriptors.FileDescriptor> dependencies = new ArrayList<>();
            fileOptionDependencyNumbers.forEach(number -> {
                if (this.fileOptionMap.containsKey(number)) {
                    dependencies.add(this.fileOptionMap.get(number));
                } else {
                    throw new RuntimeException("fileOptionMap does not contain option with number " + number);
                }
            });
            messageOptionDependencyNumbers.forEach(number -> {
                if (this.messageOptionMap.containsKey(number)) {
                    dependencies.add(this.messageOptionMap.get(number));
                } else {
                    throw new RuntimeException("messageOptionMap does not contain option with number " + number);
                }
            });
            fieldOptionDependencyNumbers.forEach(number -> {
                if (this.fieldOptionMap.containsKey(number)) {
                    dependencies.add(this.fieldOptionMap.get(number));
                } else {
                    throw new RuntimeException("fieldOptionMap does not contain option with number " + number);
                }
            });
            newFileDescriptorProto.getDependencyList().forEach(dependency -> dependencies.add(fileDescriptorMap.get(dependency)));
            Descriptors.FileDescriptor newFileDescriptor;
            try {
                newFileDescriptor = Descriptors.FileDescriptor.buildFrom(newFileDescriptorProto, dependencies.toArray(new Descriptors.FileDescriptor[0]));

            } catch (Descriptors.DescriptorValidationException e) {
                throw new RuntimeException(e);
            }

            DescriptorProtos.FileDescriptorProto.Builder newFileDescriptorProtoBuilder = DescriptorProtos.FileDescriptorProto.newBuilder(newFileDescriptorProto);
            dependencies.forEach(fd -> {
                newFileDescriptorProtoBuilder.addDependency(fd.getFullName());
            });
            setBuilder.setFile(fileIndices.get(newFileDescriptor.getFullName()), newFileDescriptorProtoBuilder.build());
        }
        return new ProtoDescriptor(setBuilder.build());
    }



}
