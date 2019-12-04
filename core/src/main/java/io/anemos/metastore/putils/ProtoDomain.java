package io.anemos.metastore.putils;

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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * PContainer is a wrapper around the FileDescriptor set. It's meant to have have easy access to
 * each Message, Enum, API, etc... that are contained within the set.
 *
 * <p>PCollections are immutable.
 */
public class ProtoDomain implements Serializable {
  public static final long serialVersionUID = 1L;
  private transient DescriptorProtos.FileDescriptorSet fileDescriptorSet;
  private transient int hashCode;

  private transient Map<String, Descriptors.FileDescriptor> fileDescriptorMap;

  private transient Map<String, Descriptors.Descriptor> descriptorMap;
  private transient Map<String, Descriptors.ServiceDescriptor> serviceMap;
  private transient Map<String, Descriptors.EnumDescriptor> enumMap;

  private transient OptionsCatalog optionsCatalog;

  ProtoDomain() {
    this(DescriptorProtos.FileDescriptorSet.newBuilder().build());
  }

  private ProtoDomain(DescriptorProtos.FileDescriptorSet fileDescriptorSet) {
    this.fileDescriptorSet = fileDescriptorSet;
    hashCode = java.util.Arrays.hashCode(this.fileDescriptorSet.toByteArray());
    crosswire();
  }

  private static Map<String, DescriptorProtos.FileDescriptorProto> extractProtoMap(
      DescriptorProtos.FileDescriptorSet fileDescriptorSet) {
    HashMap<String, DescriptorProtos.FileDescriptorProto> map = new HashMap<>();
    fileDescriptorSet.getFileList().forEach(fdp -> map.put(fdp.getName(), fdp));
    return map;
  }

  @Nullable
  private static Descriptors.FileDescriptor convertToFileDescriptorMap(
      String name,
      Map<String, DescriptorProtos.FileDescriptorProto> inMap,
      Map<String, Descriptors.FileDescriptor> outMap) {
    if (outMap.containsKey(name)) {
      return outMap.get(name);
    }
    DescriptorProtos.FileDescriptorProto fileDescriptorProto = inMap.get(name);
    if (fileDescriptorProto == null) {
      if ("google/protobuf/descriptor.proto".equals(name)) {
        outMap.put(
            "google/protobuf/descriptor.proto",
            DescriptorProtos.FieldOptions.getDescriptor().getFile());
        return DescriptorProtos.FieldOptions.getDescriptor().getFile();
      }
      return null;
    } else {
      List<Descriptors.FileDescriptor> dependencies = new ArrayList<>();
      if (fileDescriptorProto.getDependencyCount() > 0) {
        fileDescriptorProto
            .getDependencyList()
            .forEach(
                dependencyName -> {
                  Descriptors.FileDescriptor fileDescriptor =
                      convertToFileDescriptorMap(dependencyName, inMap, outMap);
                  if (fileDescriptor != null) {
                    dependencies.add(fileDescriptor);
                  }
                });
      }
      try {
        Descriptors.FileDescriptor fileDescriptor =
            Descriptors.FileDescriptor.buildFrom(
                fileDescriptorProto, dependencies.toArray(new Descriptors.FileDescriptor[0]));
        outMap.put(name, fileDescriptor);
        return fileDescriptor;
      } catch (Descriptors.DescriptorValidationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static void visitFileDescriptorTree(Map map, Descriptors.FileDescriptor fileDescriptor) {
    if (!map.containsKey(fileDescriptor.getName())) {
      map.put(fileDescriptor.getName(), fileDescriptor);
      List<Descriptors.FileDescriptor> dependencies = fileDescriptor.getDependencies();
      dependencies.forEach(fd -> visitFileDescriptorTree(map, fd));
    }
  }

  public static ProtoDomain buildFrom(List<ByteString> fileDescriptorProtoList)
      throws InvalidProtocolBufferException {
    return buildFrom(Convert.convertFileDescriptorByteStringList(fileDescriptorProtoList));
  }

  public static ProtoDomain buildFrom(Descriptors.Descriptor descriptor) {
    return buildFrom(descriptor.getFile());
  }

  public static ProtoDomain buildFrom(DescriptorProtos.FileDescriptorSet fileDescriptorSet) {
    return new ProtoDomain(fileDescriptorSet);
  }

  public static ProtoDomain buildFrom(Descriptors.FileDescriptor fileDescriptor) {
    HashMap<String, Descriptors.FileDescriptor> fileDescriptorMap = new HashMap<>();
    visitFileDescriptorTree(fileDescriptorMap, fileDescriptor);
    return buildFrom(fileDescriptorMap);
  }

  public static ProtoDomain buildFrom(Map<String, Descriptors.FileDescriptor> fileDescriptorMap) {
    DescriptorProtos.FileDescriptorSet.Builder builder =
        DescriptorProtos.FileDescriptorSet.newBuilder();
    fileDescriptorMap.values().forEach(fd -> builder.addFile(fd.toProto()));
    return new ProtoDomain(builder.build());
  }

  public static ProtoDomain buildFrom(InputStream inputStream) throws IOException {
    return buildFrom(DescriptorProtos.FileDescriptorSet.parseFrom(inputStream));
  }

  public static ProtoDomain buildFrom(File file) throws IOException {
    return buildFrom(new FileInputStream(file));
  }

  public static ProtoDomain buildFrom(byte[] buffer) throws IOException {
    return buildFrom(DescriptorProtos.FileDescriptorSet.parseFrom(buffer));
  }

  public static ProtoDomain buildFrom(ByteString buffer) throws IOException {
    return buildFrom(DescriptorProtos.FileDescriptorSet.parseFrom(buffer));
  }

  public static ProtoDomain empty() throws IOException {
    return new ProtoDomain();
  }

  private void crosswire() {
    HashMap<String, DescriptorProtos.FileDescriptorProto> map = new HashMap<>();
    fileDescriptorSet.getFileList().forEach(fdp -> map.put(fdp.getName(), fdp));

    Map<String, Descriptors.FileDescriptor> outMap = new HashMap<>();
    map.forEach((fileName, proto) -> convertToFileDescriptorMap(fileName, map, outMap));
    fileDescriptorMap = outMap;

    indexOptionsByNumber();
    indexDescriptorByName();
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

  private void indexOptionsByNumber() {
    optionsCatalog = new OptionsCatalog(fileDescriptorMap);
  }

  private void writeObject(ObjectOutputStream oos) throws IOException {
    byte[] buffer = fileDescriptorSet.toByteArray();
    oos.writeInt(buffer.length);
    oos.write(buffer);
  }

  private void readObject(ObjectInputStream ois) throws IOException {
    byte[] buffer = new byte[ois.readInt()];
    ois.readFully(buffer);
    fileDescriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(buffer);
    hashCode = java.util.Arrays.hashCode(buffer);
    crosswire();
  }

  public Descriptors.FileDescriptor getFileDescriptor(String name) {
    return fileDescriptorMap.get(name);
  }

  public Descriptors.Descriptor getDescriptor(String fullName) {
    return descriptorMap.get(fullName);
  }

  public Descriptors.FieldDescriptor getFieldOptionById(int id) {
    return optionsCatalog.fieldOptionMap.get(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProtoDomain that = (ProtoDomain) o;
    return hashCode == that.hashCode;
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCode);
  }

  public boolean contains(Descriptors.Descriptor descriptor) {
    return getDescriptor(descriptor.getFullName()) != null;
  }

  public OptionsCatalog getOptions() {
    return optionsCatalog;
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

  public ProtoDomain update(
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
            if (this.optionsCatalog.fileOptionDependencyMap.containsKey(number)) {
              dependencies.add(this.optionsCatalog.fileOptionDependencyMap.get(number));
            } else {
              throw new RuntimeException(
                  "fileOptionDependencyMap does not contain option with number " + number);
            }
          });
      messageOptionDependencyNumbers.forEach(
          number -> {
            if (this.optionsCatalog.messageOptionDependencyMap.containsKey(number)) {
              dependencies.add(this.optionsCatalog.messageOptionDependencyMap.get(number));
            } else {
              throw new RuntimeException(
                  "messageOptionDependencyMap does not contain option with number " + number);
            }
          });
      fieldOptionDependencyNumbers.forEach(
          number -> {
            if (this.optionsCatalog.fieldOptionDependencyMap.containsKey(number)) {
              dependencies.add(this.optionsCatalog.fieldOptionDependencyMap.get(number));
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
    return new ProtoDomain(setBuilder.build());
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

  public class OptionsCatalog {
    private Map<Integer, Descriptors.FieldDescriptor> fileOptionMap;
    private Map<Integer, Descriptors.FieldDescriptor> messageOptionMap;
    private Map<Integer, Descriptors.FieldDescriptor> fieldOptionMap;
    private Map<Integer, Descriptors.FieldDescriptor> serviceOptionMap;
    private Map<Integer, Descriptors.FieldDescriptor> methodOptionMap;
    private Map<Integer, Descriptors.FieldDescriptor> enumOptionMap;
    private Map<Integer, Descriptors.FieldDescriptor> enumValueOptionMap;

    private Map<Integer, Descriptors.FileDescriptor> fileOptionDependencyMap;
    private Map<Integer, Descriptors.FileDescriptor> messageOptionDependencyMap;
    private Map<Integer, Descriptors.FileDescriptor> fieldOptionDependencyMap;
    private Map<Integer, Descriptors.FileDescriptor> serviceOptionDependencyMap;
    private Map<Integer, Descriptors.FileDescriptor> methodOptionDependencyMap;
    private Map<Integer, Descriptors.FileDescriptor> enumOptionDependencyMap;
    private Map<Integer, Descriptors.FileDescriptor> enumValueOptionDependencyMap;

    OptionsCatalog(Map<String, Descriptors.FileDescriptor> fileDescriptorMap) {
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
                        case ".google.protobuf.EnumOptions":
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

    public Map<Integer, Descriptors.FieldDescriptor> getFileOptionMap() {
      return Collections.unmodifiableMap(fileOptionMap);
    }

    public Map<Integer, Descriptors.FieldDescriptor> getMessageOptionMap() {
      return Collections.unmodifiableMap(messageOptionMap);
    }

    public Map<Integer, Descriptors.FieldDescriptor> getFieldOptionMap() {
      return Collections.unmodifiableMap(fieldOptionMap);
    }

    public Map<Integer, Descriptors.FieldDescriptor> getServiceOptionMap() {
      return Collections.unmodifiableMap(serviceOptionMap);
    }

    public Map<Integer, Descriptors.FieldDescriptor> getMethodOptionMap() {
      return Collections.unmodifiableMap(methodOptionMap);
    }

    public Map<Integer, Descriptors.FieldDescriptor> getEnumOptionMap() {
      return Collections.unmodifiableMap(enumOptionMap);
    }

    public Map<Integer, Descriptors.FieldDescriptor> getEnumValueOptionMap() {
      return Collections.unmodifiableMap(enumValueOptionMap);
    }
  }
}
