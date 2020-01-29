package io.anemos.metastore.putils;

import com.google.protobuf.Any;
import com.google.protobuf.Api;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Duration;
import com.google.protobuf.Empty;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Int32Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.SourceContext;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Convert {

  private static Map<String, DescriptorProtos.FileDescriptorProto> extractProtoMap(
      DescriptorProtos.FileDescriptorSet fileDescriptorSet) {
    HashMap<String, DescriptorProtos.FileDescriptorProto> map = new HashMap<>();
    fileDescriptorSet.getFileList().forEach(fdp -> map.put(fdp.getName(), fdp));
    return map;
  }

  private static Map<String, Descriptors.FileDescriptor> convertToFileDescriptorMap(
      Map<String, DescriptorProtos.FileDescriptorProto> inMap) {
    Map<String, Descriptors.FileDescriptor> outMap = new HashMap<>();
    ExtensionRegistry registry = ExtensionRegistry.newInstance();
    inMap
        .keySet()
        .forEach(fileName -> convertToFileDescriptorMap(fileName, null, inMap, outMap, registry));
    return outMap;
  }

  private static Descriptors.FileDescriptor convertToFileDescriptorMap(
      String name,
      String parent,
      Map<String, DescriptorProtos.FileDescriptorProto> inMap,
      Map<String, Descriptors.FileDescriptor> outMap,
      ExtensionRegistry extensionRegistry) {
    if (outMap.containsKey(name)) {
      return outMap.get(name);
    }
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
        DescriptorProtos.FileDescriptorProto fileDescriptorProto = inMap.get(name);
        if (fileDescriptorProto == null) {
          if (parent == null) {
            throw new IllegalArgumentException(
                String.format("Couldn't find file \"%1\" in file descriptor set", name));
          }
          throw new IllegalArgumentException(
              String.format("Couldn't find file \"%1\", imported by \"%2\"", name, parent));
        }
        List<Descriptors.FileDescriptor> dependencies = new ArrayList<>();
        if (fileDescriptorProto.getDependencyCount() > 0) {
          fileDescriptorProto
              .getDependencyList()
              .forEach(
                  dependencyName ->
                      dependencies.add(
                          convertToFileDescriptorMap(
                              dependencyName, name, inMap, outMap, extensionRegistry)));
        }
        try {
          fd =
              Descriptors.FileDescriptor.buildFrom(
                  fileDescriptorProto, dependencies.toArray(new Descriptors.FileDescriptor[0]));

        } catch (Descriptors.DescriptorValidationException e) {
          throw new RuntimeException(e);
        }
    }
    outMap.put(name, fd);
    return fd;
  }

  static Map<String, Descriptors.FileDescriptor> convertFileDescriptorSet(
      DescriptorProtos.FileDescriptorSet fileDescriptorSet) {
    Map<String, DescriptorProtos.FileDescriptorProto> inMap = extractProtoMap(fileDescriptorSet);
    return convertToFileDescriptorMap(inMap);
  }

  static Map<String, Descriptors.FileDescriptor> convertFileDescriptorByteStringList(
      Collection<ByteString> fileDescriptorProtoList) throws InvalidProtocolBufferException {
    Map<String, DescriptorProtos.FileDescriptorProto> inMap = new HashMap<>();
    for (ByteString byteString : fileDescriptorProtoList) {
      DescriptorProtos.FileDescriptorProto fileDescriptorProto =
          DescriptorProtos.FileDescriptorProto.parseFrom(byteString);
      inMap.put(fileDescriptorProto.getName(), fileDescriptorProto);
    }
    return convertToFileDescriptorMap(inMap);
  }

  // TODO Find way to do this dynamically
  static Map<String, Descriptors.FileDescriptor> registerOptions(
      Map<String, Descriptors.FileDescriptor> fileDescriptorMap) {
    Map<String, DescriptorProtos.FileDescriptorProto> inMap = new HashMap<>();
    Map<String, Descriptors.FileDescriptor> outMap = new HashMap<>();

    fileDescriptorMap.forEach((name, fd) -> inMap.put(name, fd.toProto()));

    ExtensionRegistry registry = ExtensionRegistry.newInstance();
    inMap
        .keySet()
        .forEach(fileName -> convertToFileDescriptorMap(fileName, null, inMap, outMap, registry));

    outMap.forEach(
        (k, fileDescriptor) ->
            Descriptors.FileDescriptor.internalUpdateFileDescriptor(fileDescriptor, registry));
    return outMap;
  }
}
