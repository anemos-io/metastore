package io.anemos.metastore.core.proto;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PContainer is a wrapper around the FileDescriptor set. It's meant to have have easy access to
 * each Message, Enum, API, etc... that are contained within the set.
 *
 * <p>PCollections are immutable.
 */
public class PContainer {

  private Map<String, Descriptors.FileDescriptor> fileDescriptorMap;
  private Map<String, Descriptors.Descriptor> descriptorMap;
  private Map<String, Descriptors.ServiceDescriptor> serviceMap;
  private Map<String, Descriptors.EnumDescriptor> enumMap;
  private Map<Integer, Descriptors.FileDescriptor> fileOptionDependencyMap;
  private Map<Integer, Descriptors.FileDescriptor> messageOptionDependencyMap;
  private Map<Integer, Descriptors.FileDescriptor> fieldOptionDependencyMap;
  private Map<Integer, Descriptors.FileDescriptor> serviceOptionDependencyMap;
  private Map<Integer, Descriptors.FileDescriptor> methodOptionDependencyMap;
  private Map<Integer, Descriptors.FileDescriptor> enumOptionDependencyMap;
  private Map<Integer, Descriptors.FileDescriptor> enumValueOptionDependencyMap;

  private Map<Integer, Descriptors.FieldDescriptor> fileOptionMap;
  private Map<Integer, Descriptors.FieldDescriptor> messageOptionMap;
  private Map<Integer, Descriptors.FieldDescriptor> fieldOptionMap;
  private Map<Integer, Descriptors.FieldDescriptor> serviceOptionMap;
  private Map<Integer, Descriptors.FieldDescriptor> methodOptionMap;
  private Map<Integer, Descriptors.FieldDescriptor> enumOptionMap;
  private Map<Integer, Descriptors.FieldDescriptor> enumValueOptionMap;

  public PContainer(DescriptorProtos.FileDescriptorSet fileDescriptorSet) {
    fileDescriptorMap = Convert.convertFileDescriptorSet(fileDescriptorSet);
    indexDescriptorByName();
    indexOptionsByNumber();
  }

  public PContainer(String file) throws IOException {
    this(new File(file));
  }

  public PContainer(File file) throws IOException {
    this(new FileInputStream(file));
  }

  public PContainer() {
    this(DescriptorProtos.FileDescriptorSet.newBuilder().build());
  }

  public PContainer(InputStream inputStream) throws IOException {
    this(DescriptorProtos.FileDescriptorSet.parseFrom(inputStream));
  }

  public PContainer(byte[] buffer) throws IOException {
    this(DescriptorProtos.FileDescriptorSet.parseFrom(buffer));
  }

  public PContainer(ByteString buffer) throws IOException {
    this(DescriptorProtos.FileDescriptorSet.parseFrom(buffer));
  }

  public PContainer(Descriptors.Descriptor descriptor) {
    this(descriptor.getFile());
  }

  public PContainer(Descriptors.FileDescriptor fileDescriptor) {
    fileDescriptorMap = new HashMap<>();
    fileDescriptorMap.put(fileDescriptor.getFullName(), fileDescriptor);
    indexDescriptorByName();
    indexOptionsByNumber();
  }

  public PContainer(List<ByteString> fileDescriptorProtoList)
      throws InvalidProtocolBufferException {
    fileDescriptorMap = Convert.convertFileDescriptorByteStringList(fileDescriptorProtoList);
    indexDescriptorByName();
    indexOptionsByNumber();
  }

  public Map<Integer, Descriptors.FieldDescriptor> getFileOptionMap() {
    return Collections.unmodifiableMap(fileOptionMap);
  }

  Map<Integer, Descriptors.FieldDescriptor> getMessageOptionMap() {
    return Collections.unmodifiableMap(messageOptionMap);
  }

  Map<Integer, Descriptors.FieldDescriptor> getFieldOptionMap() {
    return Collections.unmodifiableMap(fieldOptionMap);
  }

  Map<Integer, Descriptors.FieldDescriptor> getServiceOptionMap() {
    return Collections.unmodifiableMap(serviceOptionMap);
  }

  Map<Integer, Descriptors.FieldDescriptor> getMethodOptionMap() {
    return Collections.unmodifiableMap(methodOptionMap);
  }

  Map<Integer, Descriptors.FieldDescriptor> getEnumOptionMap() {
    return Collections.unmodifiableMap(enumOptionMap);
  }

  Map<Integer, Descriptors.FieldDescriptor> getEnumValueOptionMap() {
    return Collections.unmodifiableMap(enumValueOptionMap);
  }

  public Descriptors.FileDescriptor getFileDescriptorByFileName(String fileName) {
    return fileDescriptorMap.get(fileName);
  }

  public List<Descriptors.FileDescriptor> getFileDescriptorsByPackagePrefix(String packagePrefix) {
    return fileDescriptorMap.values().stream()
        .filter(fd -> fd.getPackage().startsWith(packagePrefix))
        .collect(Collectors.toList());
  }

  public List<Descriptors.FileDescriptor> getFileDescriptorsByPackageName(String packageName) {
    return fileDescriptorMap.values().stream()
        .filter(fd -> fd.getPackage().equals(packageName))
        .collect(Collectors.toList());
  }

  public Set<String> getFileNames() {
    return fileDescriptorMap.keySet();
  }

  public List<Descriptors.FileDescriptor> getFileDescriptors() {
    return fileDescriptorMap.values().stream().collect(Collectors.toList());
  }

  public void writeToDirectory(String root) throws IOException {
    for (Map.Entry<String, Descriptors.FileDescriptor> entry : fileDescriptorMap.entrySet()) {
      String packageDir = entry.getValue().getPackage().replaceAll("\\.", "/");
      if (!packageDir.startsWith("google/protobuf")) {
        String fileName = entry.getValue().getName();
        if (fileName.contains("/")) {
          fileName =
              entry.getValue().getName().substring(entry.getValue().getName().lastIndexOf("/") + 1);
        }
        File file = new File(String.format("%s/%s/%s", root, packageDir, fileName));
        file.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(file)) {
          ProtoLanguageFileWriter.write(entry.getValue(), this, out);
        }
      }
    }
  }

  private void indexNestedDescriptorByName(List<Descriptors.Descriptor> nestedTypes) {
    nestedTypes.forEach(
        mt -> {
          descriptorMap.put(mt.getFullName(), mt);
          indexNestedDescriptorByName(mt.getNestedTypes());
        });
  }

  private void indexDescriptorByName() {
    descriptorMap = new HashMap<>();
    fileDescriptorMap.forEach(
        (k, v) ->
            v.getMessageTypes()
                .forEach(
                    mt -> {
                      descriptorMap.put(mt.getFullName(), mt);
                      indexNestedDescriptorByName(mt.getNestedTypes());
                    }));
    serviceMap = new HashMap<>();
    fileDescriptorMap.forEach(
        (k, v) ->
            v.getServices()
                .forEach(
                    mt -> {
                      serviceMap.put(mt.getFullName(), mt);
                    }));
    enumMap = new HashMap<>();
    fileDescriptorMap.forEach(
        (k, v) ->
            v.getEnumTypes()
                .forEach(
                    mt -> {
                      enumMap.put(mt.getFullName(), mt);
                    }));
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
    DescriptorProtos.FileDescriptorSet.Builder builder =
        DescriptorProtos.FileDescriptorSet.newBuilder();
    fileDescriptorMap.forEach((name, fd) -> builder.addFile(fd.toProto()));
    return builder.build().toByteArray();
  }

  public ByteString toByteString() {
    DescriptorProtos.FileDescriptorSet.Builder builder =
        DescriptorProtos.FileDescriptorSet.newBuilder();
    fileDescriptorMap.forEach((name, fd) -> builder.addFile(fd.toProto()));
    return builder.build().toByteString();
  }

  private void indexOptionsByNumber() {
    fileOptionMap = new HashMap<>();
    fileOptionDependencyMap = new HashMap<>();
    messageOptionMap = new HashMap<>();
    messageOptionDependencyMap = new HashMap<>();
    fieldOptionMap = new HashMap<>();
    fieldOptionDependencyMap = new HashMap<>();
    serviceOptionMap = new HashMap<>();
    serviceOptionDependencyMap = new HashMap<>();
    methodOptionMap = new HashMap<>();
    methodOptionDependencyMap = new HashMap<>();
    enumOptionMap = new HashMap<>();
    enumOptionDependencyMap = new HashMap<>();
    enumValueOptionMap = new HashMap<>();
    enumValueOptionDependencyMap = new HashMap<>();
    fileDescriptorMap.forEach(
        (fileName, fileDescriptor) -> {
          fileDescriptor
              .getExtensions()
              .forEach(
                  extension -> {
                    switch (extension.toProto().getExtendee()) {
                      case ".google.protobuf.FileOptions":
                        fileOptionDependencyMap.put(extension.getNumber(), fileDescriptor);
                        fileOptionMap.put(extension.getNumber(), extension);
                        break;
                      case ".google.protobuf.MessageOptions":
                        messageOptionDependencyMap.put(extension.getNumber(), fileDescriptor);
                        messageOptionMap.put(extension.getNumber(), extension);
                        break;
                      case ".google.protobuf.FieldOptions":
                        fieldOptionDependencyMap.put(extension.getNumber(), fileDescriptor);
                        fieldOptionMap.put(extension.getNumber(), extension);
                        break;
                      case ".google.protobuf.ServiceOptions":
                        serviceOptionDependencyMap.put(extension.getNumber(), fileDescriptor);
                        serviceOptionMap.put(extension.getNumber(), extension);
                        break;
                      case ".google.protobuf.MethodOptions":
                        methodOptionDependencyMap.put(extension.getNumber(), fileDescriptor);
                        methodOptionMap.put(extension.getNumber(), extension);
                        break;
                      case ".google.protobuf.Enumptions":
                        enumOptionDependencyMap.put(extension.getNumber(), fileDescriptor);
                        enumOptionMap.put(extension.getNumber(), extension);
                        break;
                      case ".google.protobuf.EnumValueOptions":
                        enumValueOptionDependencyMap.put(extension.getNumber(), fileDescriptor);
                        enumValueOptionMap.put(extension.getNumber(), extension);
                        break;
                    }
                  });
        });
  }

  public DescriptorProtos.FileDescriptorSet toFileDescriptorSet() {
    DescriptorProtos.FileDescriptorSet.Builder setBuilder =
        DescriptorProtos.FileDescriptorSet.newBuilder();
    fileDescriptorMap.forEach(
        (name, fd) -> {
          DescriptorProtos.FileDescriptorProto fileDescriptorProto =
              DescriptorProtos.FileDescriptorProto.newBuilder(fd.toProto()).build();
          setBuilder.addFile(fileDescriptorProto);
        });
    return setBuilder.build();
  }

  public PContainer update(
      Collection<DescriptorProtos.FileDescriptorProto> newFileDescriptorProtos) {
    DescriptorProtos.FileDescriptorSet fileDescriptorSet = this.toFileDescriptorSet();
    Map<String, Integer> fileIndices = new HashMap<>();
    for (int i = 0; i < fileDescriptorSet.getFileCount(); i++) {
      fileIndices.put(fileDescriptorSet.getFile(i).getName(), i);
    }
    DescriptorProtos.FileDescriptorSet.Builder setBuilder =
        DescriptorProtos.FileDescriptorSet.newBuilder(fileDescriptorSet);

    for (DescriptorProtos.FileDescriptorProto newFileDescriptorProto : newFileDescriptorProtos) {

      // TODO add recursive options
      Set<Integer> fileOptionDependencyNumbers =
          new HashSet<>(newFileDescriptorProto.getOptions().getUnknownFields().asMap().keySet());
      Set<Integer> messageOptionDependencyNumbers = new HashSet<>();
      Set<Integer> fieldOptionDependencyNumbers = new HashSet<>();
      for (DescriptorProtos.DescriptorProto descriptorProto :
          newFileDescriptorProto.getMessageTypeList()) {
        messageOptionDependencyNumbers.addAll(
            descriptorProto.getOptions().getUnknownFields().asMap().keySet());
        descriptorProto
            .getFieldList()
            .forEach(
                field ->
                    fieldOptionDependencyNumbers.addAll(
                        field.getOptions().getUnknownFields().asMap().keySet()));
      }

      Set<Descriptors.FileDescriptor> dependencies = new HashSet<>();
      fileOptionDependencyNumbers.forEach(
          number -> {
            if (this.fileOptionDependencyMap.containsKey(number)) {
              dependencies.add(this.fileOptionDependencyMap.get(number));
            } else {
              throw new RuntimeException(
                  "fileOptionDependencyMap does not contain option with number " + number);
            }
          });
      messageOptionDependencyNumbers.forEach(
          number -> {
            if (this.messageOptionDependencyMap.containsKey(number)) {
              dependencies.add(this.messageOptionDependencyMap.get(number));
            } else {
              throw new RuntimeException(
                  "messageOptionDependencyMap does not contain option with number " + number);
            }
          });
      fieldOptionDependencyNumbers.forEach(
          number -> {
            if (this.fieldOptionDependencyMap.containsKey(number)) {
              dependencies.add(this.fieldOptionDependencyMap.get(number));
            } else {
              throw new RuntimeException(
                  "fieldOptionDependencyMap does not contain option with number " + number);
            }
          });
      newFileDescriptorProto
          .getDependencyList()
          .forEach(dependency -> dependencies.add(fileDescriptorMap.get(dependency)));
      Descriptors.FileDescriptor newFileDescriptor;
      try {
        newFileDescriptor =
            Descriptors.FileDescriptor.buildFrom(
                newFileDescriptorProto, dependencies.toArray(new Descriptors.FileDescriptor[0]));

      } catch (Descriptors.DescriptorValidationException e) {
        throw new RuntimeException(e);
      }

      DescriptorProtos.FileDescriptorProto.Builder newFileDescriptorProtoBuilder =
          DescriptorProtos.FileDescriptorProto.newBuilder(newFileDescriptorProto);
      dependencies.forEach(
          fd -> {
            newFileDescriptorProtoBuilder.addDependency(fd.getFullName());
          });
      setBuilder.setFile(
          fileIndices.get(newFileDescriptor.getFullName()), newFileDescriptorProtoBuilder.build());
    }
    return new PContainer(setBuilder.build());
  }

  @Override
  public String toString() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    fileDescriptorMap.forEach(
        (name, fd) -> {
          try {
            out.write(("[ ------ " + name + " ------ ]\n").getBytes());
          } catch (IOException e) {
            e.printStackTrace();
          }

          ProtoLanguageFileWriter.write(fd, this, out);
        });
    return out.toString();
  }

  public Collection<Descriptors.FileDescriptor> iterator() {
    return fileDescriptorMap.values();
  }

  public Collection<Descriptors.FileDescriptor> getDependantFileDescriptors(
      Descriptors.FileDescriptor fd) {
    Set<Descriptors.FileDescriptor> fds = new HashSet<>();
    fds.add(fd);
    return getDependantFileDescriptors(fds);
  }

  public Collection<Descriptors.FileDescriptor> getDependantFileDescriptors(
      Collection<Descriptors.FileDescriptor> in) {
    boolean changed = false;
    Set<Descriptors.FileDescriptor> fds = new HashSet<>(in);
    for (Descriptors.FileDescriptor fileDescriptor : in) {
      boolean added = fds.addAll(fileDescriptor.getDependencies());
      changed = changed || added;
    }
    if (changed) {
      return getDependantFileDescriptors(fds);
    }
    return fds;
  }
}
