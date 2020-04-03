package io.anemos.metastore.core.proto.validate;

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
import io.anemos.metastore.v1alpha1.EnumValueChangeInfo;
import io.anemos.metastore.v1alpha1.FieldChangeInfo;
import io.anemos.metastore.v1alpha1.ImportChangeInfo;
import io.anemos.metastore.v1alpha1.MethodChangeInfo;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

  private static Map<String, Descriptors.FileDescriptor> toMap4FileDescriptor(
      Collection<Descriptors.FileDescriptor> in) {
    Map<String, Descriptors.FileDescriptor> out = new HashMap<>();
    in.forEach(
        descriptor -> {
          out.put(descriptor.getName(), descriptor);
        });
    return out;
  }

  private static Map<String, Descriptors.FieldDescriptor> toMap4FieldDescriptor(
      Collection<Descriptors.FieldDescriptor> in) {
    Map<String, Descriptors.FieldDescriptor> out = new HashMap<>();
    in.forEach(
        descriptor -> {
          out.put(String.valueOf(descriptor.getNumber()), descriptor);
        });
    return out;
  }

  private static Map<String, Descriptors.EnumValueDescriptor> toMap4EnumValueDescriptor(
      Collection<Descriptors.EnumValueDescriptor> in) {
    Map<String, Descriptors.EnumValueDescriptor> out = new HashMap<>();
    in.forEach(
        descriptor -> {
          out.put(String.valueOf(descriptor.getNumber()), descriptor);
        });
    return out;
  }

  private static Map<String, Descriptors.MethodDescriptor> toMap4MethodDescriptor(
      Collection<Descriptors.MethodDescriptor> in) {
    Map<String, Descriptors.MethodDescriptor> out = new HashMap<>();
    in.forEach(
        descriptor -> {
          out.put(String.valueOf(descriptor.getName()), descriptor);
        });
    return out;
  }

  public void diffOnFileName(String fileName) {
    Descriptors.FileDescriptor fdRef = proto_ref.getFileDescriptorByFileName(fileName);
    Descriptors.FileDescriptor fdNew = proto_new.getFileDescriptorByFileName(fileName);

    if (fdRef != null && fdNew != null) {
      diffFileDescriptor(fdRef, fdNew);
      diffOptionsFromFile(fdRef, fdNew);
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

  public void diffOnPackage(String packageName) {
    List<Descriptors.FileDescriptor> fdRef = proto_ref.getFileDescriptorsByPackageName(packageName);
    List<Descriptors.FileDescriptor> fdNew = proto_new.getFileDescriptorsByPackageName(packageName);
    diffFiles(fdRef, fdNew);
  }

  private <T extends Descriptors.GenericDescriptor> Map<String, T> toMap4Descriptor(List<T> in) {
    Map<String, T> out = new HashMap<>();
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
                      .setChangeType(ChangeType.REMOVAL)
                      .setName(v)
                      .build());
            });

    onlyInLeft(m_new, m_ref)
        .forEach(
            v ->
                results.addImportChange(
                    fullFileName,
                    ImportChangeInfo.newBuilder()
                        .setChangeType(ChangeType.ADDITION)
                        .setName(v)
                        .build()));
  }

  private <T extends Descriptors.GenericDescriptor> void diffGenericDescriptor(
      List<T> mt_ref,
      List<T> mt_new,
      Consumer<T> removal,
      Consumer<T> addition,
      BiConsumer<T, T> diff) {
    Map<String, T> m_ref = toMap4Descriptor(mt_ref);
    Map<String, T> m_new = toMap4Descriptor(mt_new);

    Set<String> onlyRef = onlyInLeft(m_ref, m_new);
    onlyRef.forEach(k -> removal.accept(m_ref.get(k)));

    Set<String> onlyNew = onlyInLeft(m_new, m_ref);
    onlyNew.forEach(k -> addition.accept(m_new.get(k)));

    Set<String> common = onlyInCommon(m_new, m_ref);
    common.forEach(k -> diff.accept(m_ref.get(k), m_new.get(k)));
  }

  private void diffMessageTypes(
      List<Descriptors.Descriptor> mt_ref, List<Descriptors.Descriptor> mt_new) {
    diffGenericDescriptor(
        mt_ref,
        mt_new,
        d ->
            results.setPatch(
                d,
                ChangeInfo.newBuilder()
                    .setChangeType(ChangeType.REMOVAL)
                    .setFromName(d.getFullName())
                    .build()),
        d ->
            results.setPatch(
                d,
                ChangeInfo.newBuilder()
                    .setChangeType(ChangeType.ADDITION)
                    .setToName(d.getFullName())
                    .build()),
        this::diffMessageType);
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
    common.forEach(k -> diffFileDescriptor(m_ref.get(k), m_new.get(k)));
  }

  private void diffMessageType(
      Descriptors.Descriptor descriptorRef, Descriptors.Descriptor descriptorNew) {
    DescriptorProtos.MessageOptions optionsRef = descriptorRef.getOptions();
    DescriptorProtos.MessageOptions optionsNew = descriptorNew.getOptions();
    diffExtensionOptions(
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
    diffFields(descriptorRef, descriptorNew);
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
                  .setChangeType(ChangeType.REMOVAL)
                  .setFromName(fd.getName())
                  .setFromTypeValue(fd.getType().toProto().getNumber())
                  .setFromDeprecated(isDeprecated(fd));
          if (d_new.isReservedNumber(fd.getNumber())) {
            builder.setChangeType(ChangeType.RESERVED);
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
                  .setChangeType(ChangeType.ADDITION)
                  .setToName(fd.getName())
                  .setToTypeValue(fd.getType().toProto().getNumber())
                  .setToDeprecated(isDeprecated(fd));
          if (d_ref.isReservedNumber(fd.getNumber())) {
            builder.setChangeType(ChangeType.UNRESERVED);
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
            results.setPatch(m_new.get(k), fieldDiff);
          }
        });
  }

  private FieldChangeInfo diffField(
      Descriptors.FieldDescriptor f_ref, Descriptors.FieldDescriptor f_new) {
    diffOptionsFromField(f_ref, f_new);
    FieldChangeInfo.Builder builder = FieldChangeInfo.newBuilder();

    if (!f_ref.getName().equals(f_new.getName())) {
      builder.setChangeType(ChangeType.CHANGED);
      builder.setFromName(f_ref.getName());
      builder.setToName(f_new.getName());
    }

    if (!f_ref.getType().equals(f_new.getType())) {
      builder.setChangeType(ChangeType.CHANGED);
      builder.setFromTypeValue(f_ref.getType().toProto().getNumber());
      builder.setToTypeValue(f_new.getType().toProto().getNumber());
      if (f_ref.getType().equals(Descriptors.FieldDescriptor.Type.MESSAGE)) {
        builder.setFromTypeName(f_ref.getMessageType().getFullName());
      }
      if (f_new.getType().equals(Descriptors.FieldDescriptor.Type.MESSAGE)) {
        builder.setFromTypeName(f_new.getMessageType().getFullName());
      }
    } else if (f_ref.getType().equals(Descriptors.FieldDescriptor.Type.MESSAGE)
        && !f_ref.getMessageType().getFullName().equals(f_new.getMessageType().getFullName())) {
      builder.setChangeType(ChangeType.CHANGED);
      builder.setFromTypeName(f_ref.getMessageType().getFullName());
      builder.setToTypeName(f_new.getMessageType().getFullName());
    }

    if (isDeprecated(f_ref) != isDeprecated(f_new)) {
      builder.setChangeType(ChangeType.CHANGED);
      builder.setFromDeprecated(isDeprecated(f_ref));
      builder.setToDeprecated(isDeprecated(f_new));
    }
    if (builder.getChangeType().equals(ChangeType.CHANGED)) {
      return builder.build();
    }
    return null;
  }

  private void diffServices(
      List<Descriptors.ServiceDescriptor> s_ref, List<Descriptors.ServiceDescriptor> s_new) {
    diffGenericDescriptor(
        s_ref,
        s_new,
        d ->
            results.setPatch(
                d,
                ChangeInfo.newBuilder()
                    .setChangeType(ChangeType.REMOVAL)
                    .setFromName(d.getFullName())
                    .build()),
        d ->
            results.setPatch(
                d,
                ChangeInfo.newBuilder()
                    .setChangeType(ChangeType.ADDITION)
                    .setToName(d.getFullName())
                    .build()),
        this::diffServiceDescriptor);
  }

  private void diffServiceDescriptor(
      Descriptors.ServiceDescriptor descriptorRef, Descriptors.ServiceDescriptor descriptorNew) {
    DescriptorProtos.ServiceOptions optionsRef = descriptorRef.getOptions();
    DescriptorProtos.ServiceOptions optionsNew = descriptorNew.getOptions();
    diffExtensionOptions(
        OptionChangeInfo.OptionType.SERVICE_OPTION,
        descriptorRef,
        optionsRef.getAllFields(),
        descriptorNew,
        optionsNew.getAllFields());
    diffUnknownOptions(
        OptionChangeInfo.OptionType.SERVICE_OPTION,
        descriptorRef,
        optionsRef.getUnknownFields(),
        descriptorNew,
        optionsNew.getUnknownFields());
    diffMethods(descriptorRef, descriptorNew);
  }

  private void diffMethods(
      Descriptors.ServiceDescriptor d_ref, Descriptors.ServiceDescriptor d_new) {
    Map<String, Descriptors.MethodDescriptor> m_ref = toMap4MethodDescriptor(d_ref.getMethods());
    Map<String, Descriptors.MethodDescriptor> m_new = toMap4MethodDescriptor(d_new.getMethods());

    Set<String> onlyRef = onlyInLeft(m_ref, m_new);
    onlyRef.forEach(
        k -> {
          Descriptors.MethodDescriptor fd = m_ref.get(k);

          MethodChangeInfo.Builder builder =
              MethodChangeInfo.newBuilder()
                  .setChangeType(ChangeType.REMOVAL)
                  .setFromName(fd.getName())
                  .setFromDeprecated(isDeprecated(fd));
          //              if (d_new.isReservedNumber(fd.getNumber())) {
          //
          // builder.setChangeType(EnumValueChangeInfo.ValueChangeType.VALUE_RESERVED);
          //                if (d_new.isReservedName(fd.getName())) {
          //                  builder.setToName(fd.getName());
          //                }
          //              }
          results.setPatch(fd, builder.build());
        });

    Set<String> onlyNew = onlyInLeft(m_new, m_ref);
    onlyNew.forEach(
        k -> {
          Descriptors.MethodDescriptor fd = m_new.get(k);
          MethodChangeInfo.Builder builder =
              MethodChangeInfo.newBuilder()
                  .setChangeType(ChangeType.ADDITION)
                  .setToName(fd.getName())
                  .setToDeprecated(isDeprecated(fd));
          //              if (d_ref.isReservedNumber(fd.getNumber())) {
          //
          // builder.setChangeType(EnumValueChangeInfo.ValueChangeType.VALUE_UNRESERVED);
          //                if (d_ref.isReservedName(fd.getName())) {
          //                  builder.setFromName(fd.getName());
          //                }
          //              }
          results.setPatch(fd, builder.build());
        });

    Set<String> common = onlyInCommon(m_new, m_ref);
    common.forEach(
        k -> {
          MethodChangeInfo fieldDiff = diffMethod(m_ref.get(k), m_new.get(k));
          if (fieldDiff != null) {
            results.setPatch(m_new.get(k), fieldDiff);
          }
        });
  }

  private MethodChangeInfo diffMethod(
      Descriptors.MethodDescriptor f_ref, Descriptors.MethodDescriptor f_new) {
    diffOptionsFromMethod(f_ref, f_new);
    MethodChangeInfo.Builder builder = MethodChangeInfo.newBuilder();

    if (!f_ref.getName().equals(f_new.getName())) {
      builder.setChangeType(ChangeType.CHANGED);
      builder.setFromName(f_ref.getName());
      builder.setToName(f_new.getName());
    }
    if (isDeprecated(f_ref) != isDeprecated(f_new)) {
      builder.setChangeType(ChangeType.CHANGED);
      builder.setFromDeprecated(isDeprecated(f_ref));
      builder.setToDeprecated(isDeprecated(f_new));
    }

    if (builder.getChangeType().equals(ChangeType.CHANGED)) {
      return builder.build();
    }
    return null;
  }

  private void diffEnumTypes(
      List<Descriptors.EnumDescriptor> e_ref, List<Descriptors.EnumDescriptor> e_new) {
    diffGenericDescriptor(
        e_ref,
        e_new,
        d ->
            results.setPatch(
                d,
                ChangeInfo.newBuilder()
                    .setChangeType(ChangeType.REMOVAL)
                    .setFromName(d.getFullName())
                    .build()),
        d ->
            results.setPatch(
                d,
                ChangeInfo.newBuilder()
                    .setChangeType(ChangeType.ADDITION)
                    .setToName(d.getFullName())
                    .build()),
        this::diffEnumDescriptor);
  }

  private void diffEnumDescriptor(
      Descriptors.EnumDescriptor descriptorRef, Descriptors.EnumDescriptor descriptorNew) {
    DescriptorProtos.EnumOptions optionsRef = descriptorRef.getOptions();
    DescriptorProtos.EnumOptions optionsNew = descriptorNew.getOptions();
    diffExtensionOptions(
        OptionChangeInfo.OptionType.ENUM_OPTION,
        descriptorRef,
        optionsRef.getAllFields(),
        descriptorNew,
        optionsNew.getAllFields());
    diffUnknownOptions(
        OptionChangeInfo.OptionType.ENUM_OPTION,
        descriptorRef,
        optionsRef.getUnknownFields(),
        descriptorNew,
        optionsNew.getUnknownFields());
    diffEnumValues(descriptorRef, descriptorNew);
  }

  private void diffEnumValues(Descriptors.EnumDescriptor d_ref, Descriptors.EnumDescriptor d_new) {
    Map<String, Descriptors.EnumValueDescriptor> m_ref =
        toMap4EnumValueDescriptor(d_ref.getValues());
    Map<String, Descriptors.EnumValueDescriptor> m_new =
        toMap4EnumValueDescriptor(d_new.getValues());

    Set<String> onlyRef = onlyInLeft(m_ref, m_new);
    onlyRef.forEach(
        k -> {
          Descriptors.EnumValueDescriptor fd = m_ref.get(k);

          EnumValueChangeInfo.Builder builder =
              EnumValueChangeInfo.newBuilder()
                  .setChangeType(ChangeType.REMOVAL)
                  .setFromName(fd.getName())
                  .setFromDeprecated(isDeprecated(fd));
          //              if (d_new.isReservedNumber(fd.getNumber())) {
          //
          // builder.setChangeType(EnumValueChangeInfo.ValueChangeType.VALUE_RESERVED);
          //                if (d_new.isReservedName(fd.getName())) {
          //                  builder.setToName(fd.getName());
          //                }
          //              }
          results.setPatch(fd, builder.build());
        });

    Set<String> onlyNew = onlyInLeft(m_new, m_ref);
    onlyNew.forEach(
        k -> {
          Descriptors.EnumValueDescriptor fd = m_new.get(k);
          EnumValueChangeInfo.Builder builder =
              EnumValueChangeInfo.newBuilder()
                  .setChangeType(ChangeType.ADDITION)
                  .setToName(fd.getName())
                  .setToDeprecated(isDeprecated(fd));
          //              if (d_ref.isReservedNumber(fd.getNumber())) {
          //
          // builder.setChangeType(EnumValueChangeInfo.ValueChangeType.VALUE_UNRESERVED);
          //                if (d_ref.isReservedName(fd.getName())) {
          //                  builder.setFromName(fd.getName());
          //                }
          //              }
          results.setPatch(fd, builder.build());
        });

    Set<String> common = onlyInCommon(m_new, m_ref);
    common.forEach(
        k -> {
          EnumValueChangeInfo fieldDiff = diffEnumValue(m_ref.get(k), m_new.get(k));
          if (fieldDiff != null) {
            results.setPatch(m_new.get(k), fieldDiff);
          }
        });
  }

  private EnumValueChangeInfo diffEnumValue(
      Descriptors.EnumValueDescriptor f_ref, Descriptors.EnumValueDescriptor f_new) {
    diffOptionsFromEnumValue(f_ref, f_new);
    EnumValueChangeInfo.Builder builder = EnumValueChangeInfo.newBuilder();

    if (!f_ref.getName().equals(f_new.getName())) {
      builder.setChangeType(ChangeType.CHANGED);
      builder.setFromName(f_ref.getName());
      builder.setToName(f_new.getName());
    }
    if (isDeprecated(f_ref) != isDeprecated(f_new)) {
      builder.setChangeType(ChangeType.CHANGED);
      builder.setFromDeprecated(isDeprecated(f_ref));
      builder.setToDeprecated(isDeprecated(f_new));
    }

    if (builder.getChangeType().equals(ChangeType.CHANGED)) {
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

  private boolean isDeprecated(Descriptors.EnumValueDescriptor fieldDescriptor) {
    Map<Descriptors.FieldDescriptor, Object> allFields =
        fieldDescriptor.getOptions().getAllFields();
    if (allFields.size() > 0) {
      for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : allFields.entrySet()) {
        Descriptors.FieldDescriptor f = entry.getKey();
        switch (f.getFullName()) {
          case "google.protobuf.EnumValueOptions.deprecated":
            return true;
        }
      }
    }
    return false;
  }

  private boolean isDeprecated(Descriptors.MethodDescriptor fieldDescriptor) {
    Map<Descriptors.FieldDescriptor, Object> allFields =
        fieldDescriptor.getOptions().getAllFields();
    if (allFields.size() > 0) {
      for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : allFields.entrySet()) {
        Descriptors.FieldDescriptor f = entry.getKey();
        switch (f.getFullName()) {
          case "google.protobuf.MethodOptions.deprecated":
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
                  .setChangeType(ChangeType.REMOVAL)
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
                  .setChangeType(ChangeType.ADDITION)
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
                    .setChangeType(ChangeType.PAYLOAD_CHANGED)
                    .setType(changeType)
                    .setPayloadOld(payloadOld)
                    .setPayloadNew(payloadNew)
                    .setOptionNumber(optionNumber);
            results.addOptionChange(descriptorNew, builder.build());
          }
        });
  }

  private void diffExtensionOptions(
      OptionChangeInfo.OptionType changeType,
      Descriptors.GenericDescriptor descriptorRef,
      Map<Descriptors.FieldDescriptor, Object> optionFieldMapRef,
      Descriptors.GenericDescriptor descriptorNew,
      Map<Descriptors.FieldDescriptor, Object> optionFieldMapNew) {

    Map<Integer, Message> fieldsRef =
        optionFieldMapRef.entrySet().stream()
            .collect(
                Collectors.toMap(
                    k -> k.getKey().getNumber(),
                    v ->
                        DynamicMessage.newBuilder(v.getKey().getContainingType())
                            .setField(v.getKey(), v.getValue())
                            .build()));
    Map<Integer, Message> fieldsNew =
        optionFieldMapNew.entrySet().stream()
            .collect(
                Collectors.toMap(
                    k -> k.getKey().getNumber(),
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
                  .setChangeType(ChangeType.REMOVAL)
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
                  .setChangeType(ChangeType.ADDITION)
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
                    .setChangeType(ChangeType.PAYLOAD_CHANGED)
                    .setType(changeType)
                    .setOptionNumber(optionNumber)
                    .setPayloadOld(payloadRef)
                    .setPayloadNew(payloadNew);
            results.addOptionChange(descriptorNew, builder.build());
          }
        });
  }

  private void diffOptionsFromFile(
      Descriptors.FileDescriptor descriptorRef, Descriptors.FileDescriptor descriptorNew) {
    DescriptorProtos.FileOptions optionsRef = descriptorRef.getOptions();
    DescriptorProtos.FileOptions optionsNew = descriptorNew.getOptions();
    diffExtensionOptions(
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

  private void diffOptionsFromField(
      Descriptors.FieldDescriptor descriptorRef, Descriptors.FieldDescriptor descriptorNew) {
    DescriptorProtos.FieldOptions optionsRef = descriptorRef.getOptions();
    DescriptorProtos.FieldOptions optionsNew = descriptorNew.getOptions();
    diffExtensionOptions(
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

  private void diffOptionsFromEnumValue(
      Descriptors.EnumValueDescriptor descriptorRef,
      Descriptors.EnumValueDescriptor descriptorNew) {
    DescriptorProtos.EnumValueOptions optionsRef = descriptorRef.getOptions();
    DescriptorProtos.EnumValueOptions optionsNew = descriptorNew.getOptions();
    diffExtensionOptions(
        OptionChangeInfo.OptionType.ENUM_VALUE_OPTION,
        descriptorRef,
        optionsRef.getAllFields(),
        descriptorNew,
        optionsNew.getAllFields());
    diffUnknownOptions(
        OptionChangeInfo.OptionType.ENUM_VALUE_OPTION,
        descriptorRef,
        optionsRef.getUnknownFields(),
        descriptorNew,
        optionsNew.getUnknownFields());
  }

  private void diffOptionsFromMethod(
      Descriptors.MethodDescriptor descriptorRef, Descriptors.MethodDescriptor descriptorNew) {
    DescriptorProtos.MethodOptions optionsRef = descriptorRef.getOptions();
    DescriptorProtos.MethodOptions optionsNew = descriptorNew.getOptions();
    diffExtensionOptions(
        OptionChangeInfo.OptionType.METHOD_OPTION,
        descriptorRef,
        optionsRef.getAllFields(),
        descriptorNew,
        optionsNew.getAllFields());
    diffUnknownOptions(
        OptionChangeInfo.OptionType.METHOD_OPTION,
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
    } catch (IOException e) {
      throw new RuntimeException("failed to serialize unknown field with number ", e);
    }
    return ByteString.copyFrom(byteBuffer);
  }
}
