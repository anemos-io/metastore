package io.anemos.metastore.core.proto.shadow;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.UnknownFieldSet;
import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.v1alpha1.FieldResult;
import io.anemos.metastore.v1alpha1.FileResult;
import io.anemos.metastore.v1alpha1.MessageResult;
import io.anemos.metastore.v1alpha1.OptionChangeInfo;
import io.anemos.metastore.v1alpha1.Report;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;

public class ShadowRegistry {

  private static final Logger LOG = Logger.getLogger(ShadowRegistry.class.getName());

  private ProtoDescriptor defaultDescriptor;
  private ProtoDescriptor shadow;
  private Report delta;
  private Git shadowRepo;
  private String path = "/tmp/shadow-contracts";

  public ShadowRegistry(ProtoDescriptor defaultDescriptor, Report delta) {
    this.defaultDescriptor = defaultDescriptor;
    this.delta = delta;
    applyDelta(delta);
    initRepo();
  }

  public void applyDelta(Report delta) {
    try {
      initShadow();

      HashMap<String, DescriptorProtos.FileDescriptorProto.Builder> fileDescriptorProtoBuilders =
          new HashMap<>();
      applyMessageResults(delta, fileDescriptorProtoBuilders);
      applyFileResults(delta, fileDescriptorProtoBuilders);
      List<DescriptorProtos.FileDescriptorProto> fileDescriptorProtos = new ArrayList<>();
      fileDescriptorProtoBuilders.forEach(
          (name, fd) -> {
            fileDescriptorProtos.add(fd.build());
          });
      shadow = shadow.update(fileDescriptorProtos);
    } catch (Exception e) {
      throw new RuntimeException("Failed to apply delta", e);
    }
  }

  private void applyFileResults(
      Report delta,
      HashMap<String, DescriptorProtos.FileDescriptorProto.Builder> fileDescriptorProtoBuilders) {
    for (Map.Entry<String, FileResult> fileResultEntry : delta.getFileResultsMap().entrySet()) {
      Descriptors.FileDescriptor fileDescriptor =
          shadow.getFileDescriptorByFileName(fileResultEntry.getKey());

      DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder =
          getOrCreateFileDescriptorProtoBuilder(
              fileDescriptorProtoBuilders, fileResultEntry.getKey(), fileDescriptor);
      fileDescriptorProtoBuilder.mergeFrom(
          applyFileOptionChanges(fileDescriptor, fileResultEntry.getValue().getOptionChangeList()));
    }
  }

  private void applyMessageResults(
      Report delta,
      HashMap<String, DescriptorProtos.FileDescriptorProto.Builder> fileDescriptorProtoBuilders) {
    for (Map.Entry<String, MessageResult> messageResultEntry :
        delta.getMessageResultsMap().entrySet()) {
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

  private DescriptorProtos.FileDescriptorProto applyFileOptionChanges(
      Descriptors.FileDescriptor fileDescriptor, List<OptionChangeInfo> optionChanges) {
    DescriptorProtos.FileDescriptorProto.Builder newDescriptorBuilder =
        fileDescriptor.toProto().toBuilder();
    UnknownFieldSet unknownFieldSet = buildUnknownFieldSet(optionChanges);
    DescriptorProtos.FileOptions fileOptions =
        DescriptorProtos.FileOptions.newBuilder().setUnknownFields(unknownFieldSet).build();
    return newDescriptorBuilder.setOptions(fileOptions).clearMessageType().build();
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
        case OPTION_ADDED:
          unknownFieldSetBuilder =
              mergeUnknownField(unknownFieldSetBuilder, optionChange.getPayloadNew());
          break;
        case OPTION_REMOVED:
          unknownFieldSetBuilder.clearField(optionChange.getOptionNumber());
          break;
        case OPTION_PAYLOAD_CHANGED:
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

  private void initShadow() throws IOException {
    this.shadow = new ProtoDescriptor(defaultDescriptor.toByteArray());
  }

  private void initRepo() {
    if (System.getenv("DEBUG") != null && System.getenv("DEBUG").equals("true")) {
      return;
    }
    final File localPath = new File(path);
    try {
      if (localPath.exists()) {
        FileUtils.forceDelete(localPath);
      }
      this.shadowRepo =
          Git.cloneRepository()
              .setURI(System.getenv("METASTORE_SHADOW_REPO_URI"))
              .setDirectory(localPath)
              .call();
    } catch (Exception e) {
      throw new RuntimeException("Can't init local shadow repo", e);
    }
  }

  public void sync(ProtoDescriptor defaultDescriptor) {
    if (System.getenv("DEBUG") != null && System.getenv("DEBUG").equals("true")) {
      return;
    }
    try {
      defaultDescriptor.writeToDirectory(path);

      // TODO Apply delta's here

      shadowRepo.pull();
      shadowRepo.add().addFilepattern(".").call();
      Status status = shadowRepo.status().call();
      if (status.hasUncommittedChanges()) {
        shadowRepo.commit().setMessage("contract sync").call();
        shadowRepo.push().call();
        LOG.info("contract sync");
      } else {
        LOG.info("no changes to commit");
      }

    } catch (Exception e) {
      throw new RuntimeException("Failed syncing the shadow repo", e);
    }

    // Add delta's.
    // git add, commit, push
  }

  public Report getDelta() {
    return this.delta;
  }

  public ProtoDescriptor getShadow() {
    applyDelta(this.delta);
    return this.shadow;
  }

  public void setDelta(Report delta) {
    this.delta = delta;
  }
}
