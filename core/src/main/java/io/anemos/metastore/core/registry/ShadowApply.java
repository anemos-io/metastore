package io.anemos.metastore.core.registry;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.UnknownFieldSet;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.FieldResult;
import io.anemos.metastore.v1alpha1.FileResult;
import io.anemos.metastore.v1alpha1.ImportChangeInfo;
import io.anemos.metastore.v1alpha1.MessageResult;
import io.anemos.metastore.v1alpha1.OptionChangeInfo;
import io.anemos.metastore.v1alpha1.Patch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ShadowApply {

  private ProtoDomain shadow;

  public ProtoDomain applyPatch(ProtoDomain defaultDescriptor, Patch patch) {
    try {
      shadow = defaultDescriptor;

      HashMap<String, DescriptorProtos.FileDescriptorProto.Builder> fileDescriptorProtoBuilders =
          new HashMap<>();
      applyMessageResults(patch, fileDescriptorProtoBuilders);
      applyFileResults(patch, fileDescriptorProtoBuilders);
      List<DescriptorProtos.FileDescriptorProto> fileDescriptorProtos = new ArrayList<>();
      fileDescriptorProtoBuilders.forEach(
          (name, fd) -> {
            fileDescriptorProtos.add(fd.build());
          });
      return shadow.toBuilder().merge(fileDescriptorProtos).build();
    } catch (Exception e) {
      throw new RuntimeException("Failed to apply patch", e);
    }
  }

  private void applyFileResults(
      Patch patch,
      HashMap<String, DescriptorProtos.FileDescriptorProto.Builder> fileDescriptorProtoBuilders) {
    for (Map.Entry<String, FileResult> fileResultEntry : patch.getFileResultsMap().entrySet()) {
      Descriptors.FileDescriptor fileDescriptor =
          shadow.getFileDescriptorByFileName(fileResultEntry.getKey());

      DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder =
          getOrCreateFileDescriptorProtoBuilder(
              fileDescriptorProtoBuilders, fileResultEntry.getKey(), fileDescriptor);
      fileDescriptorProtoBuilder.mergeFrom(
          applyImportChanges(
                  applyFileOptionChanges(
                      fileDescriptor, fileResultEntry.getValue().getOptionChangeList()),
                  fileResultEntry.getValue().getImportChangeList())
              .build());
    }
  }

  private void applyMessageResults(
      Patch patch,
      HashMap<String, DescriptorProtos.FileDescriptorProto.Builder> fileDescriptorProtoBuilders) {
    for (Map.Entry<String, MessageResult> messageResultEntry :
        patch.getMessageResultsMap().entrySet()) {
      Descriptors.Descriptor descriptor = shadow.getDescriptorByName(messageResultEntry.getKey());
      HashMap<String, Integer> messageNameToIndexMap =
          getMessageNameToIndexMap(descriptor.getFile());
      DescriptorProtos.DescriptorProto.Builder newDescriptorProtoBuilder =
          DescriptorProtos.DescriptorProto.newBuilder(descriptor.toProto());
      applyFieldResults(messageResultEntry.getValue(), descriptor, newDescriptorProtoBuilder);

      if (messageResultEntry.getValue().getOptionChangeCount() > 0) {
        applyMessageOptionChanges(
            newDescriptorProtoBuilder,
            descriptor,
            messageResultEntry.getValue().getOptionChangeList());
      }
      DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder =
          getOrCreateFileDescriptorProtoBuilder(
              fileDescriptorProtoBuilders,
              descriptor.getFile().getFullName(),
              descriptor.getFile());
      fileDescriptorProtoBuilder.setMessageType(
          messageNameToIndexMap.get(descriptor.getFullName()), newDescriptorProtoBuilder);
    }
  }

  private DescriptorProtos.FileDescriptorProto.Builder getOrCreateFileDescriptorProtoBuilder(
      HashMap<String, DescriptorProtos.FileDescriptorProto.Builder> fileDescriptorProtoBuilders,
      String name,
      Descriptors.FileDescriptor fileDescriptor) {
    DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder;
    if (fileDescriptorProtoBuilders.containsKey(name)) {
      fileDescriptorProtoBuilder = fileDescriptorProtoBuilders.get(name);
    } else {
      fileDescriptorProtoBuilder =
          DescriptorProtos.FileDescriptorProto.newBuilder(fileDescriptor.toProto());
      fileDescriptorProtoBuilders.put(
          fileDescriptorProtoBuilder.getName(), fileDescriptorProtoBuilder);
    }
    return fileDescriptorProtoBuilder;
  }

  private void applyFieldResults(
      MessageResult messageResult,
      Descriptors.Descriptor messageDescriptor,
      DescriptorProtos.DescriptorProto.Builder newDescriptorProtoBuilder) {
    HashMap<Integer, Integer> fieldNumberToIndexMap = getFieldNumberToIndexMap(messageDescriptor);
    for (FieldResult fieldResult : messageResult.getFieldResultsList()) {
      if (fieldResult.getOptionChangeCount() > 0) {
        Descriptors.FieldDescriptor fieldDescriptor =
            messageDescriptor.findFieldByNumber(fieldResult.getNumber());
        DescriptorProtos.FieldDescriptorProto newFieldDescriptorProto =
            applyFieldOptionChanges(fieldDescriptor, fieldResult.getOptionChangeList());
        newDescriptorProtoBuilder.setField(
            fieldNumberToIndexMap.get(fieldResult.getNumber()), newFieldDescriptorProto);
      }
    }
  }

  private HashMap<Integer, Integer> getFieldNumberToIndexMap(Descriptors.Descriptor descriptor) {
    HashMap<Integer, Integer> indexMap = new HashMap<>();
    for (int i = 0; i < descriptor.getFields().size(); i++) {
      Descriptors.FieldDescriptor fieldDescriptor = descriptor.getFields().get(i);
      indexMap.put(fieldDescriptor.getNumber(), i);
    }
    return indexMap;
  }

  private HashMap<String, Integer> getMessageNameToIndexMap(
      Descriptors.FileDescriptor fileDescriptor) {
    HashMap<String, Integer> indexMap = new HashMap<>();
    for (int i = 0; i < fileDescriptor.getMessageTypes().size(); i++) {
      Descriptors.Descriptor descriptor = fileDescriptor.getMessageTypes().get(i);
      indexMap.put(descriptor.getFullName(), i);
    }
    return indexMap;
  }

  private DescriptorProtos.FileDescriptorProto.Builder applyFileOptionChanges(
      Descriptors.FileDescriptor fileDescriptor, List<OptionChangeInfo> optionChanges) {
    DescriptorProtos.FileDescriptorProto.Builder newDescriptorBuilder =
        fileDescriptor.toProto().toBuilder();
    UnknownFieldSet unknownFieldSet = buildUnknownFieldSet(optionChanges);
    DescriptorProtos.FileOptions fileOptions =
        DescriptorProtos.FileOptions.newBuilder().setUnknownFields(unknownFieldSet).build();
    return newDescriptorBuilder.setOptions(fileOptions).clearMessageType();
  }

  private DescriptorProtos.FileDescriptorProto.Builder applyImportChanges(
      DescriptorProtos.FileDescriptorProto.Builder builder, List<ImportChangeInfo> importChanges) {

    Set<String> dependencyList = new HashSet<>(builder.getDependencyList());
    importChanges.forEach(
        changeInfo -> {
          switch (changeInfo.getChangeType()) {
            case ADDITION:
              dependencyList.add(changeInfo.getName());
              break;
            case REMOVAL:
              dependencyList.remove(changeInfo.getName());
              break;
            case UNCHANGED:
            case UNRECOGNIZED:
          }
        });
    builder.clearDependency();
    builder.addAllDependency(dependencyList);
    return builder;
  }

  private DescriptorProtos.DescriptorProto applyMessageOptionChanges(
      DescriptorProtos.DescriptorProto.Builder newDescriptorBuilder,
      Descriptors.Descriptor descriptor,
      List<OptionChangeInfo> optionChanges) {
    UnknownFieldSet unknownFieldSet = buildUnknownFieldSet(optionChanges);
    DescriptorProtos.MessageOptions messageOptions =
        DescriptorProtos.MessageOptions.newBuilder().setUnknownFields(unknownFieldSet).build();
    return newDescriptorBuilder.setOptions(messageOptions).build();
  }

  private DescriptorProtos.FieldDescriptorProto applyFieldOptionChanges(
      Descriptors.FieldDescriptor fieldDescriptor, List<OptionChangeInfo> optionChanges) {
    DescriptorProtos.FieldDescriptorProto.Builder newFieldDescriptorProtoBuilder =
        fieldDescriptor.toProto().toBuilder();
    UnknownFieldSet unknownFieldSet = buildUnknownFieldSet(optionChanges);

    DescriptorProtos.FieldOptions fieldOptions =
        DescriptorProtos.FieldOptions.newBuilder().setUnknownFields(unknownFieldSet).build();

    return newFieldDescriptorProtoBuilder.setOptions(fieldOptions).build();
  }

  private UnknownFieldSet buildUnknownFieldSet(List<OptionChangeInfo> optionChanges) {
    UnknownFieldSet.Builder unknownFieldSetBuilder = UnknownFieldSet.newBuilder();
    for (OptionChangeInfo optionChange : optionChanges) {
      switch (optionChange.getChangeType()) {
        case ADDITION:
          unknownFieldSetBuilder =
              mergeUnknownField(unknownFieldSetBuilder, optionChange.getPayloadNew());
          break;
        case REMOVAL:
          unknownFieldSetBuilder.clearField(optionChange.getOptionNumber());
          break;
        case PAYLOAD_CHANGED:
          unknownFieldSetBuilder.clearField(optionChange.getOptionNumber());
          unknownFieldSetBuilder =
              mergeUnknownField(unknownFieldSetBuilder, optionChange.getPayloadNew());
          break;
      }
    }
    return unknownFieldSetBuilder.build();
  }

  private UnknownFieldSet.Builder mergeUnknownField(
      UnknownFieldSet.Builder globalUnknownFieldSetBuilder, ByteString payload) {
    try {
      return globalUnknownFieldSetBuilder.mergeFrom(payload);
    } catch (Exception e) {
      throw new RuntimeException("merge of unknown field failed", e);
    }
  }
}
