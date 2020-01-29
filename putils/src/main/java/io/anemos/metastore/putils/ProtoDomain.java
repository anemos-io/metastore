package io.anemos.metastore.putils;

import com.google.protobuf.Any;
import com.google.protobuf.Api;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Duration;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Empty;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Int32Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.SourceContext;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Type;
import com.google.protobuf.UnknownFieldSet;
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
      Map<String, Descriptors.FileDescriptor> outMap,
      ExtensionRegistry registry) {
    if (outMap.containsKey(name)) {
      return outMap.get(name);
    }
    DescriptorProtos.FileDescriptorProto fileDescriptorProto = inMap.get(name);
    if (fileDescriptorProto == null) {
      Descriptors.FileDescriptor fd;
      switch (name) {
        case "google/protobuf/descriptor.proto":
          fd = DescriptorProtos.FieldOptions.getDescriptor().getFile();
          break;
        case "google/protobuf/wrappers.proto":
          fd = Int32Value.getDescriptor().getFile();
          break;
        case "google/protobuf/timestamp.proto":
          fd = Timestamp.getDescriptor().getFile();
          break;
        case "google/protobuf/duration.proto":
          fd = Duration.getDescriptor().getFile();
          break;
        case "google/protobuf/any.proto":
          fd = Any.getDescriptor().getFile();
          break;
        case "google/protobuf/api.proto":
          fd = Api.getDescriptor().getFile();
          break;
        case "google/protobuf/empty.proto":
          fd = Empty.getDescriptor().getFile();
          break;
        case "google/protobuf/field_mask.proto":
          fd = FieldMask.getDescriptor().getFile();
          break;
        case "google/protobuf/source_context.proto":
          fd = SourceContext.getDescriptor().getFile();
          break;
        case "google/protobuf/struct.proto":
          fd = Struct.getDescriptor().getFile();
          break;
        case "google/protobuf/type.proto":
          fd = Type.getDescriptor().getFile();
          break;
        default:
          return null;
      }
      outMap.put(name, fd);
      return fd;
    } else {
      List<Descriptors.FileDescriptor> dependencies = new ArrayList<>();
      if (fileDescriptorProto.getDependencyCount() > 0) {
        fileDescriptorProto
            .getDependencyList()
            .forEach(
                dependencyName -> {
                  Descriptors.FileDescriptor fileDescriptor =
                      convertToFileDescriptorMap(dependencyName, inMap, outMap, registry);
                  if (fileDescriptor != null) {
                    dependencies.add(fileDescriptor);
                  }
                });
      }
      try {
        Descriptors.FileDescriptor fileDescriptor =
            Descriptors.FileDescriptor.buildFrom(
                fileDescriptorProto, dependencies.toArray(new Descriptors.FileDescriptor[0]));
        fileDescriptor
            .getExtensions()
            .forEach(
                extension -> {
                  if (extension.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
                    registry.add(
                        extension, DynamicMessage.newBuilder(extension.getMessageType()).build());
                  } else {
                    registry.add(extension);
                  }
                });
        Descriptors.FileDescriptor.internalUpdateFileDescriptor(fileDescriptor, registry);
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

  public static ProtoDomain buildFrom(Collection<ByteString> fileDescriptorProtoList)
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

  public static ProtoDomain empty() {
    return new ProtoDomain();
  }

  public Builder toBuilder() {
    return new Builder(this.fileDescriptorMap);
  }

  public static Builder builder() {
    return new Builder(new HashMap<>());
  }

  private void crosswire() {
    HashMap<String, DescriptorProtos.FileDescriptorProto> map = new HashMap<>();
    fileDescriptorSet.getFileList().stream()
        .filter(fdp -> !fdp.getName().startsWith("google/protobuf"))
        .forEach(fdp -> map.put(fdp.getName(), fdp));

    Map<String, Descriptors.FileDescriptor> outMap = new HashMap<>();
    ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
    map.forEach(
        (fileName, proto) -> convertToFileDescriptorMap(fileName, map, outMap, extensionRegistry));
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

  public DescriptorProtos.FileDescriptorSet getFileDescriptorSet() {
    return fileDescriptorSet;
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
    return new ArrayList<>(fileDescriptorMap.values());
  }

  public void writeToDirectory(String root) throws IOException {
    for (Descriptors.FileDescriptor fd : fileDescriptorMap.values()) {
      String fileName = fd.getFullName();
      if (!fileName.startsWith("google/protobuf")) {
        File file = new File(String.format("%s/%s", root, fileName));
        file.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(file)) {
          ProtoLanguageFileWriter.write(fd, this, out);
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

  public Collection<Descriptors.Descriptor> findDescriptorsByOption(String optionName) {
    Descriptors.FieldDescriptor fieldDescriptor = optionsCatalog.getMessageOptionByName(optionName);
    return descriptorMap.values().stream()
        .filter(
            descriptor -> {
              DescriptorProtos.MessageOptions options = descriptor.getOptions();
              UnknownFieldSet.Field unknown =
                  options.getUnknownFields().getField(fieldDescriptor.getNumber());
              if (unknown.getLengthDelimitedList().size()
                      + unknown.getFixed64List().size()
                      + unknown.getFixed32List().size()
                      + unknown.getVarintList().size()
                  > 0) {
                return true;
              }
              return options.getAllFields().containsKey(fieldDescriptor);
            })
        .collect(Collectors.toList());
  }

  public Collection<Descriptors.FileDescriptor> findFileDescriptorsByOption(String optionName) {
    Descriptors.FieldDescriptor fieldDescriptor = optionsCatalog.getFileOptionByName(optionName);
    return fileDescriptorMap.values().stream()
        .filter(
            descriptor -> {
              DescriptorProtos.FileOptions options = descriptor.getOptions();
              UnknownFieldSet.Field unknown =
                  options.getUnknownFields().getField(fieldDescriptor.getNumber());
              if (unknown.getLengthDelimitedList().size()
                      + unknown.getFixed64List().size()
                      + unknown.getFixed32List().size()
                      + unknown.getVarintList().size()
                  > 0) {
                return true;
              }
              return options.getAllFields().containsKey(fieldDescriptor);
            })
        .collect(Collectors.toList());
  }

  public Collection<Descriptors.EnumDescriptor> findEnumDescriptorsByOption(String optionName) {
    Descriptors.FieldDescriptor fieldDescriptor = optionsCatalog.getEnumOptionByName(optionName);
    return enumMap.values().stream()
        .filter(
            descriptor -> {
              DescriptorProtos.EnumOptions options = descriptor.getOptions();
              UnknownFieldSet.Field unknown =
                  options.getUnknownFields().getField(fieldDescriptor.getNumber());
              if (unknown.getLengthDelimitedList().size()
                      + unknown.getFixed64List().size()
                      + unknown.getFixed32List().size()
                      + unknown.getVarintList().size()
                  > 0) {
                return true;
              }
              return options.getAllFields().containsKey(fieldDescriptor);
            })
        .collect(Collectors.toList());
  }

  public Collection<Descriptors.ServiceDescriptor> findServiceDescriptorsByOption(
      String optionName) {
    Descriptors.FieldDescriptor fieldDescriptor = optionsCatalog.getServiceOptionByName(optionName);
    return serviceMap.values().stream()
        .filter(
            descriptor -> {
              DescriptorProtos.ServiceOptions options = descriptor.getOptions();
              UnknownFieldSet.Field unknown =
                  options.getUnknownFields().getField(fieldDescriptor.getNumber());
              if (unknown.getLengthDelimitedList().size()
                      + unknown.getFixed64List().size()
                      + unknown.getFixed32List().size()
                      + unknown.getVarintList().size()
                  > 0) {
                return true;
              }
              return options.getAllFields().containsKey(fieldDescriptor);
            })
        .collect(Collectors.toList());
  }

  public static class Builder {
    private Map<String, DescriptorProtos.FileDescriptorProto> fileDescriptorMap;

    public Builder(Map<String, Descriptors.FileDescriptor> in) {
      fileDescriptorMap =
          in.values().stream()
              .map(p -> p.toProto())
              .collect(Collectors.toMap(DescriptorProtos.FileDescriptorProto::getName, v -> v));
    }

    private Map<String, DescriptorProtos.FileDescriptorProto> toMap(
        Collection<ByteString> updateBytes) throws InvalidProtocolBufferException {
      Map<String, DescriptorProtos.FileDescriptorProto> map = new HashMap<>();
      for (ByteString updateByte : updateBytes) {
        DescriptorProtos.FileDescriptorProto proto =
            DescriptorProtos.FileDescriptorProto.parseFrom(updateByte);
        map.put(proto.getName(), proto);
      }
      return map;
    }

    public Builder mergeBinary(Collection<ByteString> updateBytes)
        throws InvalidProtocolBufferException {
      Collection<DescriptorProtos.FileDescriptorProto> updateProtos = new ArrayList<>();
      for (ByteString bs : updateBytes) {
        updateProtos.add(DescriptorProtos.FileDescriptorProto.parseFrom(bs));
      }
      return merge(updateProtos);
    }

    public Builder replaceFileBinary(String file, Collection<ByteString> updateBytes)
        throws InvalidProtocolBufferException {
      Map<String, DescriptorProtos.FileDescriptorProto> map = toMap(updateBytes);
      if (map.containsKey(file)) {
        add(map.get(file));
      } else {
        fileDescriptorMap.remove(file);
      }
      return this;
    }

    public Builder replacePackageBinary(String packageName, Collection<ByteString> updateBytes)
        throws InvalidProtocolBufferException {
      Map<String, DescriptorProtos.FileDescriptorProto> updated = toMap(updateBytes);
      List<String> removing = new ArrayList<>();
      // clear the package
      fileDescriptorMap.forEach(
          (k, v) -> {
            if (v.getPackage().equals(packageName)) {
              removing.add(k);
            }
          });
      removing.forEach(f -> fileDescriptorMap.remove(f));
      // only add with package prefix
      updated.forEach(
          (fileName, fdp) -> {
            if (fdp.getPackage().equals(packageName)) {
              fileDescriptorMap.put(fileName, fdp);
            }
          });
      return this;
    }

    private boolean isInPackagePrefix(
        DescriptorProtos.FileDescriptorProto fdp, String packagePrefix) {
      if (fdp.getPackage().equals(packagePrefix)) {
        return true;
      } else if (fdp.getPackage().startsWith(packagePrefix + ".")) {
        return true;
      }
      return false;
    }

    public Builder replacePackagePrefixBinary(
        String packagePrefix, Collection<ByteString> updateBytes)
        throws InvalidProtocolBufferException {
      Map<String, DescriptorProtos.FileDescriptorProto> updated = toMap(updateBytes);
      List<String> removing = new ArrayList<>();
      // clear the package prefix
      fileDescriptorMap.forEach(
          (fileName, fdp) -> {
            if (isInPackagePrefix(fdp, packagePrefix)) {
              removing.add(fileName);
            }
          });
      removing.forEach(f -> fileDescriptorMap.remove(f));
      // only add in package
      updated.forEach(
          (fileName, fdp) -> {
            if (isInPackagePrefix(fdp, packagePrefix)) {
              fileDescriptorMap.put(fileName, fdp);
            }
          });
      return this;
    }

    public Builder merge(Collection<DescriptorProtos.FileDescriptorProto> updateProtos) {
      updateProtos.forEach(proto -> add(proto));
      return this;
    }

    public Builder add(DescriptorProtos.FileDescriptorProto proto) {
      fileDescriptorMap.put(proto.getName(), proto);
      return this;
    }

    public ProtoDomain build() throws InvalidProtocolBufferException {
      return ProtoDomain.buildFrom(
          fileDescriptorMap.values().stream()
              .map(v -> v.toByteString())
              .collect(Collectors.toList()));
    }
  }

  public static class OptionsCatalog {
    private Map<Integer, Descriptors.FieldDescriptor> fileOptionMap;
    private Map<Integer, Descriptors.FieldDescriptor> messageOptionMap;
    private Map<Integer, Descriptors.FieldDescriptor> fieldOptionMap;
    private Map<Integer, Descriptors.FieldDescriptor> serviceOptionMap;
    private Map<Integer, Descriptors.FieldDescriptor> methodOptionMap;
    private Map<Integer, Descriptors.FieldDescriptor> enumOptionMap;
    private Map<Integer, Descriptors.FieldDescriptor> enumValueOptionMap;

    OptionsCatalog(Map<String, Descriptors.FileDescriptor> fileDescriptorMap) {
      fileOptionMap = new HashMap<>();
      messageOptionMap = new HashMap<>();
      fieldOptionMap = new HashMap<>();
      serviceOptionMap = new HashMap<>();
      methodOptionMap = new HashMap<>();
      enumOptionMap = new HashMap<>();
      enumValueOptionMap = new HashMap<>();

      fileDescriptorMap.forEach(
          (fileName, fileDescriptor) -> {
            fileDescriptor
                .getExtensions()
                .forEach(
                    extension -> {
                      switch (extension.toProto().getExtendee()) {
                        case ".google.protobuf.FileOptions":
                          fileOptionMap.put(extension.getNumber(), extension);
                          break;
                        case ".google.protobuf.MessageOptions":
                          messageOptionMap.put(extension.getNumber(), extension);
                          break;
                        case ".google.protobuf.FieldOptions":
                          fieldOptionMap.put(extension.getNumber(), extension);
                          break;
                        case ".google.protobuf.ServiceOptions":
                          serviceOptionMap.put(extension.getNumber(), extension);
                          break;
                        case ".google.protobuf.MethodOptions":
                          methodOptionMap.put(extension.getNumber(), extension);
                          break;
                        case ".google.protobuf.EnumOptions":
                          enumOptionMap.put(extension.getNumber(), extension);
                          break;
                        case ".google.protobuf.EnumValueOptions":
                          enumValueOptionMap.put(extension.getNumber(), extension);
                          break;
                      }
                    });
          });
    }

    Map<Integer, Descriptors.FieldDescriptor> getFileOptionMap() {
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

    public Descriptors.FieldDescriptor getFileOptionByName(String fullName) {
      return findByName(fileOptionMap, fullName);
    }

    public Descriptors.FieldDescriptor getMessageOptionByName(String fullName) {
      return findByName(messageOptionMap, fullName);
    }

    public Descriptors.FieldDescriptor getFieldOptionByName(String fullName) {
      return findByName(fieldOptionMap, fullName);
    }

    public Descriptors.FieldDescriptor getEnumOptionByName(String fullName) {
      return findByName(enumOptionMap, fullName);
    }

    public Descriptors.FieldDescriptor getEnumValueOptionByName(String fullName) {
      return findByName(enumValueOptionMap, fullName);
    }

    public Descriptors.FieldDescriptor getServiceOptionByName(String fullName) {
      return findByName(serviceOptionMap, fullName);
    }

    public Descriptors.FieldDescriptor getMethodOptionByName(String fullName) {
      return findByName(methodOptionMap, fullName);
    }

    private Descriptors.FieldDescriptor findByName(
        Map<Integer, Descriptors.FieldDescriptor> map, String fullName) {
      return map.values().stream()
          .filter(fd -> fd.getFullName().equals(fullName))
          .findFirst()
          .get();
    }
  }
}
