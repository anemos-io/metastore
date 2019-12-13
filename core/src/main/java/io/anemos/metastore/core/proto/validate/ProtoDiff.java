package io.anemos.metastore.core.proto.validate;

import static io.anemos.metastore.v1alpha1.FieldChangeInfo.FieldChangeType.FIELD_ADDED;
import static io.anemos.metastore.v1alpha1.FieldChangeInfo.FieldChangeType.FIELD_CHANGED;
import static io.anemos.metastore.v1alpha1.FieldChangeInfo.FieldChangeType.FIELD_REMOVED;
import static io.anemos.metastore.v1alpha1.FieldChangeInfo.FieldChangeType.FIELD_RESERVED;
import static io.anemos.metastore.v1alpha1.FieldChangeInfo.FieldChangeType.FIELD_UNRESERVED;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.UnknownFieldSet;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.ChangeInfo;
import io.anemos.metastore.v1alpha1.ChangeType;
import io.anemos.metastore.v1alpha1.FieldChangeInfo;
import io.anemos.metastore.v1alpha1.ImportChangeInfo;
import io.anemos.metastore.v1alpha1.OptionChangeInfo;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * File in package -> can be removed/added/keep the same Message -> can move from
 * file/added/removed/keep the same Message content (fields) -> can keep the same/added/removed
 */
public class ProtoDiff {

  private ProtoDomain proto_ref;
  private ProtoDomain proto_new;
  private ValidationResults results;

  public ProtoDiff(ProtoDomain fd_ref, ProtoDomain fd_new, ValidationResults results) {
    this.proto_ref = fd_ref;
    this.proto_new = fd_new;
    this.results = results;
  }

  static Map<String, Descriptors.FileDescriptor> toMap4FileDescriptor(
      Collection<Descriptors.FileDescriptor> in) {
    Map<String, Descriptors.FileDescriptor> out = new HashMap<>();
    in.forEach(
        descriptor -> {
          out.put(descriptor.getName(), descriptor);
        });
    return out;
  }

  static Map<String, Descriptors.FieldDescriptor> toMap4FieldDescriptor(
      Collection<Descriptors.FieldDescriptor> in) {
    Map<String, Descriptors.FieldDescriptor> out = new HashMap<>();
    in.forEach(
        descriptor -> {
          out.put(String.valueOf(descriptor.getNumber()), descriptor);
        });
    return out;
  }

  public void diffOnFileName(String fileName) {
    Descriptors.FileDescriptor fdRef = proto_ref.getFileDescriptorByFileName(fileName);
    Descriptors.FileDescriptor fdNew = proto_new.getFileDescriptorByFileName(fileName);

    if (fdRef != null && fdNew != null) {
      diffFileDescriptor(fdRef, fdNew);
      diffFileOptions(fdRef, fdNew);
    }
  }

  private void diffFileDescriptor(
      @Nullable Descriptors.FileDescriptor fdRef, @Nullable Descriptors.FileDescriptor fdNew) {
    List<Descriptors.Descriptor> refDescriptors;
    List<Descriptors.EnumDescriptor> refEnumDescriptors;
    List<Descriptors.ServiceDescriptor> refServiceDescriptors;
    List<String> refDependencies;
    List<Descriptors.Descriptor> newDescriptors;
    List<Descriptors.EnumDescriptor> newEnumDescriptors;
    List<Descriptors.ServiceDescriptor> newServiceDescriptors;
    List<String> newDependencies;
    String fileName = null;
    if (fdRef != null) {
      fileName = fdRef.getFullName();
      refDescriptors = fdRef.getMessageTypes();
      refEnumDescriptors = fdRef.getEnumTypes();
      refServiceDescriptors = fdRef.getServices();
      refDependencies =
          fdRef.getDependencies().stream()
              .map(Descriptors.FileDescriptor::getFullName)
              .collect(Collectors.toList());
    } else {
      results.setPatch(fdNew, ChangeInfo.newBuilder().setChangeType(ChangeType.ADDITION).build());
      refDescriptors = new ArrayList<>(0);
      refEnumDescriptors = new ArrayList<>(0);
      refServiceDescriptors = new ArrayList<>(0);
      refDependencies = new ArrayList<>(0);
    }

    if (fdNew != null) {
      fileName = fdNew.getFullName();
      newDescriptors = fdNew.getMessageTypes();
      newEnumDescriptors = fdNew.getEnumTypes();
      newServiceDescriptors = fdNew.getServices();
      newDependencies =
          fdNew.getDependencies().stream()
              .map(Descriptors.FileDescriptor::getFullName)
              .collect(Collectors.toList());
    } else {
      results.setPatch(fdRef, ChangeInfo.newBuilder().setChangeType(ChangeType.REMOVAL).build());
      newDescriptors = new ArrayList<>(0);
      newEnumDescriptors = new ArrayList<>(0);
      newServiceDescriptors = new ArrayList<>(0);
      newDependencies = new ArrayList<>(0);
    }

    diffMessageTypes(refDescriptors, newDescriptors);
    diffEnumTypes(refEnumDescriptors, newEnumDescriptors);
    diffServices(refServiceDescriptors, newServiceDescriptors);
    diffImports(fileName, refDependencies, newDependencies);
  }

  public void diffOnMessage(String messageName) {
    Descriptors.Descriptor refDescriptor = proto_ref.getDescriptorByName(messageName);
    Descriptors.Descriptor newDescriptor = proto_new.getDescriptorByName(messageName);

    diffMessageType(refDescriptor, newDescriptor);
  }

  public void diffOnPackagePrefix(String packagePrefix) {
    List<Descriptors.FileDescriptor> fdRef =
        proto_ref.getFileDescriptorsByPackagePrefix(packagePrefix);
    List<Descriptors.FileDescriptor> fdNew =
        proto_new.getFileDescriptorsByPackagePrefix(packagePrefix);
    diffFiles(fdRef, fdNew);
  }

  // TODO implement diff services
  private void diffServices(
      List<Descriptors.ServiceDescriptor> s_ref, List<Descriptors.ServiceDescriptor> s_new) {}

  // TODO implement diff enum types
  private void diffEnumTypes(
      List<Descriptors.EnumDescriptor> e_ref, List<Descriptors.EnumDescriptor> e_new) {}

  private Map<String, Descriptors.Descriptor> toMap4Descriptor(List<Descriptors.Descriptor> in) {
    Map<String, Descriptors.Descriptor> out = new HashMap<>();
    in.forEach(
        descriptor -> {
          out.put(descriptor.getName(), descriptor);
        });
    return out;
  }

  private <T> Set<T> onlyInLeft(Map<T, ?> left, Map<T, ?> right) {
    HashSet<T> integers = new HashSet<T>(left.keySet());
    integers.removeAll(right.keySet());
    return integers;
  }

  private <T> Set<T> onlyInCommon(Map<T, ?> left, Map<T, ?> right) {
    Set<T> intersect = new HashSet<>(left.keySet());
    intersect.addAll(right.keySet());
    intersect.removeAll(onlyInLeft(left, right));
    intersect.removeAll(onlyInLeft(right, left));
    return intersect;
  }

  private void diffImports(String fullFileName, List<String> c_ref, List<String> c_new) {
    Map<String, String> m_ref =
        c_ref.stream().collect(Collectors.toMap(String::toString, String::toString));
    Map<String, String> m_new =
        c_new.stream().collect(Collectors.toMap(String::toString, String::toString));

    onlyInLeft(m_ref, m_new)
        .forEach(
            v -> {
              results.addImportChange(
                  fullFileName,
                  ImportChangeInfo.newBuilder()
                      .setChangeType(ImportChangeInfo.ImportChangeType.IMPORT_REMOVED)
                      .setName(v)
                      .build());
            });

    onlyInLeft(m_new, m_ref)
        .forEach(
            v -> {
              results.addImportChange(
                  fullFileName,
                  ImportChangeInfo.newBuilder()
                      .setChangeType(ImportChangeInfo.ImportChangeType.IMPORT_ADDED)
                      .setName(v)
                      .build());
            });
  }

  private void diffMessageTypes(
      List<Descriptors.Descriptor> mt_ref, List<Descriptors.Descriptor> mt_new) {
    Map<String, Descriptors.Descriptor> m_ref = toMap4Descriptor(mt_ref);
    Map<String, Descriptors.Descriptor> m_new = toMap4Descriptor(mt_new);

    Set<String> onlyRef = onlyInLeft(m_ref, m_new);
    onlyRef.forEach(
        k -> {
          Descriptors.Descriptor fd = m_ref.get(k);
          results.setPatch(
              fd,
              ChangeInfo.newBuilder()
                  .setChangeType(ChangeType.REMOVAL)
                  .setFromName(fd.getFullName())
                  .build());
        });

    Set<String> onlyNew = onlyInLeft(m_new, m_ref);
    onlyNew.forEach(
        k -> {
          Descriptors.Descriptor fd = m_new.get(k);
          results.setPatch(
              fd,
              ChangeInfo.newBuilder()
                  .setChangeType(ChangeType.ADDITION)
                  .setFromName(fd.getFullName())
                  .build());
        });

    Set<String> common = onlyInCommon(m_new, m_ref);
    common.forEach(
        k -> {
          diffMessageType(m_ref.get(k), m_new.get(k));
        });
  }

  private void diffFiles(
      List<Descriptors.FileDescriptor> f_ref, List<Descriptors.FileDescriptor> f_new) {
    Map<String, Descriptors.FileDescriptor> m_ref = toMap4FileDescriptor(f_ref);
    Map<String, Descriptors.FileDescriptor> m_new = toMap4FileDescriptor(f_new);

    Set<String> onlyRef = onlyInLeft(m_ref, m_new);
    onlyRef.forEach(
        k -> {
          Descriptors.FileDescriptor fd = m_ref.get(k);
          results.setPatch(
              fd,
              ChangeInfo.newBuilder()
                  .setChangeType(ChangeType.REMOVAL)
                  .setFromName(fd.getName())
                  .build());
          diffFileDescriptor(fd, null);
        });

    Set<String> onlyNew = onlyInLeft(m_new, m_ref);
    onlyNew.forEach(
        k -> {
          Descriptors.FileDescriptor fd = m_new.get(k);
          results.setPatch(
              fd,
              ChangeInfo.newBuilder()
                  .setChangeType(ChangeType.ADDITION)
                  .setToName(fd.getName())
                  .build());
          diffFileDescriptor(null, fd);
        });

    Set<String> common = onlyInCommon(m_new, m_ref);
    common.forEach(
        k -> {
          diffFileDescriptor(m_ref.get(k), m_new.get(k));
        });
  }

  private void diffMessageType(Descriptors.Descriptor d_ref, Descriptors.Descriptor d_new) {
    diffMessageOptions(d_ref, d_new);
    diffFields(d_ref, d_new);
  }

  private void diffFields(Descriptors.Descriptor d_ref, Descriptors.Descriptor d_new) {
    Map<String, Descriptors.FieldDescriptor> m_ref = toMap4FieldDescriptor(d_ref.getFields());
    Map<String, Descriptors.FieldDescriptor> m_new = toMap4FieldDescriptor(d_new.getFields());

    Set<String> onlyRef = onlyInLeft(m_ref, m_new);
    onlyRef.forEach(
        k -> {
          Descriptors.FieldDescriptor fd = m_ref.get(k);
          FieldChangeInfo.Builder builder =
              FieldChangeInfo.newBuilder()
                  .setChangeType(FIELD_REMOVED)
                  .setFromName(fd.getName())
                  .setFromTypeValue(fd.getType().toProto().getNumber())
                  .setFromDeprecated(isDeprecated(fd));
          if (d_new.isReservedNumber(fd.getNumber())) {
            builder.setChangeType(FIELD_RESERVED);
            if (d_new.isReservedName(fd.getName())) {
              builder.setToName(fd.getName());
            }
          }
          results.setPatch(fd, builder.build());
        });

    Set<String> onlyNew = onlyInLeft(m_new, m_ref);
    onlyNew.forEach(
        k -> {
          Descriptors.FieldDescriptor fd = m_new.get(k);
          FieldChangeInfo.Builder builder =
              FieldChangeInfo.newBuilder()
                  .setChangeType(FIELD_ADDED)
                  .setToName(fd.getName())
                  .setToTypeValue(fd.getType().toProto().getNumber())
                  .setToDeprecated(isDeprecated(fd));
          if (d_ref.isReservedNumber(fd.getNumber())) {
            builder.setChangeType(FIELD_UNRESERVED);
            if (d_ref.isReservedName(fd.getName())) {
              builder.setFromName(fd.getName());
            }
          }
          results.setPatch(fd, builder.build());
        });

    Set<String> common = onlyInCommon(m_new, m_ref);
    common.forEach(
        k -> {
          FieldChangeInfo fieldDiff = diffField(m_ref.get(k), m_new.get(k));
          if (fieldDiff != null) {
            results.setPatch(m_ref.get(k), fieldDiff);
          }
        });
  }

  private FieldChangeInfo diffField(
      Descriptors.FieldDescriptor f_ref, Descriptors.FieldDescriptor f_new) {
    diffFieldOptions(f_ref, f_new);
    FieldChangeInfo.Builder builder = FieldChangeInfo.newBuilder();

    if (!f_ref.getName().equals(f_new.getName())) {
      builder.setChangeType(FIELD_CHANGED);
      builder.setFromName(f_ref.getName());
      builder.setToName(f_new.getName());
    }
    if (!f_ref.getType().equals(f_new.getType())) {
      builder.setChangeType(FIELD_CHANGED);
      builder.setFromTypeValue(f_ref.getType().toProto().getNumber());
      builder.setToTypeValue(f_new.getType().toProto().getNumber());
    }
    if (isDeprecated(f_ref) != isDeprecated(f_new)) {
      builder.setChangeType(FIELD_CHANGED);
      builder.setFromDeprecated(isDeprecated(f_ref));
      builder.setToDeprecated(isDeprecated(f_new));
    }

    if (builder.getChangeType().equals(FIELD_CHANGED)) {
      return builder.build();
    }
    return null;
  }

  private boolean isDeprecated(Descriptors.FieldDescriptor fieldDescriptor) {
    Map<Descriptors.FieldDescriptor, Object> allFields =
        fieldDescriptor.getOptions().getAllFields();
    if (allFields.size() > 0) {
      for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : allFields.entrySet()) {
        Descriptors.FieldDescriptor f = entry.getKey();
        switch (f.getFullName()) {
          case "google.protobuf.FieldOptions.deprecated":
            return true;
        }
      }
    }
    return false;
  }

  private void diffUnknownOptions(
      OptionChangeInfo.OptionType changeType,
      Descriptors.GenericDescriptor descriptorRef,
      UnknownFieldSet unknownFieldSetRef,
      Descriptors.GenericDescriptor descriptorNew,
      UnknownFieldSet unknownFieldSetNew) {

    Map<Integer, UnknownFieldSet.Field> fieldsRef = unknownFieldSetRef.asMap();
    Map<Integer, UnknownFieldSet.Field> fieldsNew = unknownFieldSetNew.asMap();

    Set<Integer> onlyInLeft = onlyInLeft(fieldsRef, fieldsNew);
    onlyInLeft.forEach(
        optionNumber -> {
          UnknownFieldSet.Field field = fieldsRef.get(optionNumber);
          ByteString payload = serializeUnknownField(optionNumber, field);
          OptionChangeInfo.Builder builder =
              OptionChangeInfo.newBuilder()
                  .setChangeType(OptionChangeInfo.OptionChangeType.OPTION_REMOVED)
                  .setType(changeType)
                  .setOptionNumber(optionNumber)
                  .setPayloadNew(payload);
          results.addOptionChange(descriptorRef, builder.build());
        });

    Set<Integer> onlyNew = onlyInLeft(fieldsNew, fieldsRef);
    onlyNew.forEach(
        optionNumber -> {
          UnknownFieldSet.Field field = fieldsNew.get(optionNumber);
          ByteString payload = serializeUnknownField(optionNumber, field);
          OptionChangeInfo.Builder builder =
              OptionChangeInfo.newBuilder()
                  .setChangeType(OptionChangeInfo.OptionChangeType.OPTION_ADDED)
                  .setType(changeType)
                  .setOptionNumber(optionNumber)
                  .setPayloadNew(payload);
          results.addOptionChange(descriptorNew, builder.build());
        });

    Set<Integer> common = onlyInCommon(fieldsRef, fieldsNew);
    common.forEach(
        optionNumber -> {
          UnknownFieldSet.Field fieldOld = fieldsRef.get(optionNumber);
          UnknownFieldSet.Field fieldNew = fieldsNew.get(optionNumber);

          ByteString payloadOld = serializeUnknownField(optionNumber, fieldOld);
          ByteString payloadNew = serializeUnknownField(optionNumber, fieldNew);
          if (!payloadOld.equals(payloadNew)) {
            OptionChangeInfo.Builder builder =
                OptionChangeInfo.newBuilder()
                    .setChangeType(OptionChangeInfo.OptionChangeType.OPTION_PAYLOAD_CHANGED)
                    .setType(changeType)
                    .setPayloadOld(payloadOld)
                    .setPayloadNew(payloadNew)
                    .setOptionNumber(optionNumber);
            results.addOptionChange(descriptorNew, builder.build());
          }
        });
  }

  private void diffOptions(
      OptionChangeInfo.OptionType changeType,
      Descriptors.GenericDescriptor descriptorRef,
      Map<Descriptors.FieldDescriptor, Object> optionFieldMapRef,
      Descriptors.GenericDescriptor descriptorNew,
      Map<Descriptors.FieldDescriptor, Object> optionFieldMapNew) {

    Map<Integer, Message> fieldsRef =
        optionFieldMapRef.entrySet().stream()
            .collect(
                Collectors.toMap(
                    k -> k.getKey().getNumber(), // k.getKey().toString(),
                    v ->
                        DynamicMessage.newBuilder(v.getKey().getContainingType())
                            .setField(v.getKey(), v.getValue())
                            .build()));
    Map<Integer, Message> fieldsNew =
        optionFieldMapNew.entrySet().stream()
            .collect(
                Collectors.toMap(
                    k -> k.getKey().getNumber(), // k.getKey().toString(),
                    v ->
                        DynamicMessage.newBuilder(v.getKey().getContainingType())
                            .setField(v.getKey(), v.getValue())
                            .build()));

    Set<Integer> onlyInLeft = onlyInLeft(fieldsRef, fieldsNew);
    onlyInLeft.forEach(
        optionNumber -> {
          Message field = fieldsRef.get(optionNumber);
          ByteString payload = serializePayload(field);
          OptionChangeInfo.Builder builder =
              OptionChangeInfo.newBuilder()
                  .setChangeType(OptionChangeInfo.OptionChangeType.OPTION_REMOVED)
                  .setType(changeType)
                  .setOptionNumber(optionNumber)
                  .setPayloadNew(payload);
          results.addOptionChange(descriptorRef, builder.build());
        });

    Set<Integer> onlyNew = onlyInLeft(fieldsNew, fieldsRef);
    onlyNew.forEach(
        optionNumber -> {
          Message field = fieldsNew.get(optionNumber);
          ByteString payload = serializePayload(field);
          OptionChangeInfo.Builder builder =
              OptionChangeInfo.newBuilder()
                  .setChangeType(OptionChangeInfo.OptionChangeType.OPTION_ADDED)
                  .setType(changeType)
                  .setOptionNumber(optionNumber)
                  .setPayloadNew(payload);
          results.addOptionChange(descriptorNew, builder.build());
        });

    Set<Integer> common = onlyInCommon(fieldsRef, fieldsNew);
    common.forEach(
        optionNumber -> {
          Message fieldRef = fieldsRef.get(optionNumber);
          Message fieldNew = fieldsNew.get(optionNumber);
          ByteString payloadRef = serializePayload(fieldRef);
          ByteString payloadNew = serializePayload(fieldNew);
          if (!payloadRef.equals(payloadNew)) {
            OptionChangeInfo.Builder builder =
                OptionChangeInfo.newBuilder()
                    .setChangeType(OptionChangeInfo.OptionChangeType.OPTION_PAYLOAD_CHANGED)
                    .setType(changeType)
                    .setOptionNumber(optionNumber)
                    .setPayloadOld(payloadRef)
                    .setPayloadNew(payloadNew);
            results.addOptionChange(descriptorNew, builder.build());
          }
        });

    System.out.println();
  }

  private void diffFileOptions(
      Descriptors.FileDescriptor descriptorRef, Descriptors.FileDescriptor descriptorNew) {
    DescriptorProtos.FileOptions optionsRef = descriptorRef.getOptions();
    DescriptorProtos.FileOptions optionsNew = descriptorNew.getOptions();
    diffOptions(
        OptionChangeInfo.OptionType.FILE_OPTION,
        descriptorRef,
        optionsRef.getAllFields(),
        descriptorNew,
        optionsNew.getAllFields());
    diffUnknownOptions(
        OptionChangeInfo.OptionType.FILE_OPTION,
        descriptorRef,
        optionsRef.getUnknownFields(),
        descriptorNew,
        optionsNew.getUnknownFields());
  }

  private void diffMessageOptions(
      Descriptors.Descriptor descriptorRef, Descriptors.Descriptor descriptorNew) {
    DescriptorProtos.MessageOptions optionsRef = descriptorRef.getOptions();
    DescriptorProtos.MessageOptions optionsNew = descriptorNew.getOptions();
    diffOptions(
        OptionChangeInfo.OptionType.MESSAGE_OPTION,
        descriptorRef,
        optionsRef.getAllFields(),
        descriptorNew,
        optionsNew.getAllFields());
    diffUnknownOptions(
        OptionChangeInfo.OptionType.MESSAGE_OPTION,
        descriptorRef,
        optionsRef.getUnknownFields(),
        descriptorNew,
        optionsNew.getUnknownFields());
  }

  private void diffFieldOptions(
      Descriptors.FieldDescriptor descriptorRef, Descriptors.FieldDescriptor descriptorNew) {
    DescriptorProtos.FieldOptions optionsRef = descriptorRef.getOptions();
    DescriptorProtos.FieldOptions optionsNew = descriptorNew.getOptions();
    diffOptions(
        OptionChangeInfo.OptionType.FIELD_OPTION,
        descriptorRef,
        optionsRef.getAllFields(),
        descriptorNew,
        optionsNew.getAllFields());
    diffUnknownOptions(
        OptionChangeInfo.OptionType.FIELD_OPTION,
        descriptorRef,
        optionsRef.getUnknownFields(),
        descriptorNew,
        optionsNew.getUnknownFields());
  }

  private ByteString serializeUnknownField(int optionNumber, UnknownFieldSet.Field field) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(field.getSerializedSize(optionNumber));
    CodedOutputStream stream = CodedOutputStream.newInstance(byteBuffer);
    try {
      field.writeTo(optionNumber, stream);
    } catch (IOException e) {
      throw new RuntimeException(
          "failed to serialize unknown field with number " + optionNumber, e);
    }
    return ByteString.copyFrom(byteBuffer);
  }

  private ByteString serializePayload(Message field) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(field.getSerializedSize());
    try {
      CodedOutputStream stream = CodedOutputStream.newInstance(byteBuffer);
      field.writeTo(stream);
      //
      //    try {
      //      switch (field.get.getType()) {
      //        case DOUBLE:
      //          break;
      //        case FLOAT:
      //          break;
      //        case INT64:
      //          break;
      //        case UINT64:
      //          break;
      //        case INT32:
      //          break;
      //        case FIXED64:
      //          break;
      //        case FIXED32:
      //          break;
      //        case BOOL:
      //          break;
      //        case STRING:
      //          break;
      //        case GROUP:
      //          break;
      //        case MESSAGE:
      //          stream.writeBytes(fieldDescriptor.getNumber(), ((Message) value).toByteString());
      //          break;
      //        case BYTES:
      //          break;
      //        case UINT32:
      //          break;
      //        case ENUM:
      //          break;
      //        case SFIXED32:
      //          break;
      //        case SFIXED64:
      //          break;
      //        case SINT32:
      //          break;
      //        case SINT64:
      //          break;
      //      }
      //
    } catch (IOException e) {
      throw new RuntimeException("failed to serialize unknown field with number ", e);
    }
    return ByteString.copyFrom(byteBuffer);
  }
}
