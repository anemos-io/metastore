package io.anemos.metastore.putils;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
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
    inMap.forEach((k, v) -> convertToFileDescriptorMap(k, inMap, outMap, registry));
    return outMap;
  }

  private static Descriptors.FileDescriptor convertToFileDescriptorMap(
      String name,
      Map<String, DescriptorProtos.FileDescriptorProto> inMap,
      Map<String, Descriptors.FileDescriptor> outMap,
      ExtensionRegistry extensionRegistry) {
    if (outMap.containsKey(name)) {
      return outMap.get(name);
    }
    Descriptors.FileDescriptor fileDescriptor;
    if ("google/protobuf/descriptor.proto".equals(name)) {
      fileDescriptor = DescriptorProtos.getDescriptor();
    } else {
      DescriptorProtos.FileDescriptorProto fileDescriptorProto = inMap.get(name);
      List<Descriptors.FileDescriptor> dependencies = new ArrayList<>();
      if (fileDescriptorProto.getDependencyCount() > 0) {
        fileDescriptorProto
            .getDependencyList()
            .forEach(
                dependencyName ->
                    dependencies.add(
                        convertToFileDescriptorMap(
                            dependencyName, inMap, outMap, extensionRegistry)));
      }
      try {
        fileDescriptor =
            Descriptors.FileDescriptor.buildFrom(
                fileDescriptorProto, dependencies.toArray(new Descriptors.FileDescriptor[0]));

      } catch (Descriptors.DescriptorValidationException e) {
        throw new RuntimeException(e);
      }
    }
    outMap.put(name, fileDescriptor);
    return fileDescriptor;
  }

  static Map<String, Descriptors.FileDescriptor> convertFileDescriptorSet(
      DescriptorProtos.FileDescriptorSet fileDescriptorSet) {
    Map<String, DescriptorProtos.FileDescriptorProto> inMap = extractProtoMap(fileDescriptorSet);
    return convertToFileDescriptorMap(inMap);
  }

  static Map<String, Descriptors.FileDescriptor> convertFileDescriptorByteStringList(
      List<ByteString> fileDescriptorProtoList) throws InvalidProtocolBufferException {
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
    inMap.forEach((k, v) -> convertToFileDescriptorMap(k, inMap, outMap, registry));

    outMap.forEach(
        (k, fileDescriptor) ->
            Descriptors.FileDescriptor.internalUpdateFileDescriptor(fileDescriptor, registry));
    return outMap;
  }
}
