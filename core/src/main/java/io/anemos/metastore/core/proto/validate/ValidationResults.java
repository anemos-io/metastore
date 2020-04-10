package io.anemos.metastore.core.proto.validate;

import com.google.protobuf.Descriptors;
import io.anemos.metastore.v1alpha1.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationResults {
  private Map<String, FilePatchContainer> fileMap = new HashMap<>();
  private Map<String, MessagePatchContainer> messageMap = new HashMap<>();
  private Map<String, EnumPatchContainer> enumMap = new HashMap<>();
  private Map<String, ServicePatchContainer> serviceMap = new HashMap<>();

  public List<RuleInfo> getInfo(String messageName, String fieldName) {
    List<RuleInfo> rules = new ArrayList<>();
    MessagePatchContainer messageResult = messageMap.get(messageName);
    if (messageResult != null) {
      FieldPatchContainer fieldResultContainer = messageResult.fieldMap.get(fieldName);
      if (fieldResultContainer != null) {
        rules.addAll(fieldResultContainer.info);
      }
    }
    return rules;
  }

  private MessagePatchContainer getOrCreateMessage(Descriptors.Descriptor descriptor) {
    String messageName = descriptor.getFullName();
    MessagePatchContainer messageResult = messageMap.get(messageName);
    if (messageResult == null) {
      messageResult = new MessagePatchContainer();
      messageResult.packageName = descriptor.getFile().getPackage();
      messageResult.fullName = messageName;
      messageMap.put(messageName, messageResult);
    }
    return messageResult;
  }

  private ServicePatchContainer getOrCreateService(
      Descriptors.ServiceDescriptor serviceDescriptor) {
    String serviceName = serviceDescriptor.getFullName();
    ServicePatchContainer serviceResult = serviceMap.get(serviceName);
    if (serviceResult == null) {
      serviceResult = new ServicePatchContainer();
      serviceResult.packageName = serviceDescriptor.getFile().getPackage();
      serviceResult.fullName = serviceName;
      serviceMap.put(serviceName, serviceResult);
    }
    return serviceResult;
  }

  private FilePatchContainer getOrCreateFile(String fileName) {
    FilePatchContainer fileResult = fileMap.get(fileName);
    if (fileResult == null) {
      fileResult = new FilePatchContainer();
      fileResult.fullName = fileName;
      fileMap.put(fileName, fileResult);
    }
    return fileResult;
  }

  private EnumPatchContainer getOrCreateEnum(Descriptors.EnumDescriptor enumDescriptor) {
    String fileName = enumDescriptor.getFullName();
    EnumPatchContainer enumResult = enumMap.get(fileName);
    if (enumResult == null) {
      enumResult = new EnumPatchContainer();
      enumResult.packageName = enumDescriptor.getFile().getPackage();
      enumResult.fullName = fileName;
      enumMap.put(fileName, enumResult);
    }
    return enumResult;
  }

  void addResult(Descriptors.FieldDescriptor fd, RuleInfo ruleInfo) {
    MessagePatchContainer messageResult = getOrCreateMessage(fd.getContainingType());
    messageResult.add(fd, ruleInfo);
  }

  void addResult(Descriptors.MethodDescriptor md, RuleInfo ruleInfo) {
    ServicePatchContainer messageResult = getOrCreateService(md.getService());
    messageResult.add(md, ruleInfo);
  }

  void addResult(Descriptors.Descriptor descriptor, RuleInfo ruleInfo) {
    MessagePatchContainer messageResult = getOrCreateMessage(descriptor);
    messageResult.addResult(ruleInfo);
  }

  void addResult(Descriptors.ServiceDescriptor descriptor, RuleInfo ruleInfo) {
    ServicePatchContainer serviceResult = getOrCreateService(descriptor);
    serviceResult.addResult(ruleInfo);
  }

  void addResult(Descriptors.FileDescriptor descriptor, RuleInfo ruleInfo) {
    FilePatchContainer fileResult = getOrCreateFile(descriptor.getFullName());
    fileResult.addResult(ruleInfo);
  }

  void setPatch(Descriptors.FieldDescriptor fd, FieldChange patch) {
    MessagePatchContainer resultContainer = getOrCreateMessage(fd.getContainingType());
    resultContainer.addPatch(fd, patch);
  }

  void setPatch(Descriptors.MethodDescriptor fd, MethodChange patch) {
    ServicePatchContainer resultContainer = getOrCreateService(fd.getService());
    resultContainer.addPatch(fd, patch);
  }

  void setPatch(Descriptors.EnumValueDescriptor fd, EnumValueChange patch) {
    EnumPatchContainer resultContainer = getOrCreateEnum(fd.getType());
    resultContainer.addPatch(fd, patch);
  }

  void setPatch(Descriptors.Descriptor fd, Change patch) {
    MessagePatchContainer resultContainer = getOrCreateMessage(fd);
    resultContainer.setPatch(patch);
  }

  void setPatch(Descriptors.FileDescriptor fd, Change patch) {
    FilePatchContainer resultContainer = getOrCreateFile(fd.getFullName());
    resultContainer.setPatch(patch);
  }

  void setPatch(Descriptors.EnumDescriptor fd, Change patch) {
    EnumPatchContainer resultContainer = getOrCreateEnum(fd);
    resultContainer.setPatch(patch);
  }

  void setPatch(Descriptors.ServiceDescriptor fd, Change patch) {
    ServicePatchContainer serviceResult = getOrCreateService(fd);
    serviceResult.setPatch(patch);
  }

  void addOptionChange(Descriptors.GenericDescriptor descriptor, OptionChange info) {
    if (descriptor instanceof Descriptors.FileDescriptor) {
      FilePatchContainer fileResultContainer = getOrCreateFile(descriptor.getFullName());
      fileResultContainer.addOptionChange(info);
    } else if (descriptor instanceof Descriptors.Descriptor) {
      MessagePatchContainer messageResult = getOrCreateMessage((Descriptors.Descriptor) descriptor);
      messageResult.addOptionChange(info);
    } else if (descriptor instanceof Descriptors.FieldDescriptor) {
      Descriptors.FieldDescriptor fieldDescriptor = (Descriptors.FieldDescriptor) descriptor;
      MessagePatchContainer messageResult = getOrCreateMessage(fieldDescriptor.getContainingType());
      messageResult.addOptionChange(fieldDescriptor, info);
    } else {
      // TODO
      throw new RuntimeException("Unimplemented option");
    }
  }

  void addImportChange(String fullName, ImportChange info) {
    FilePatchContainer fileResultContainer = getOrCreateFile(fullName);
    fileResultContainer.addImportChange(info);
  }

  public Patch createProto() {
    Patch.Builder builder = Patch.newBuilder();
    fileMap.values().forEach(file -> builder.putFilePatches(file.fullName, file.createProto()));
    messageMap
        .values()
        .forEach(message -> builder.putMessagePatches(message.fullName, message.createProto()));
    serviceMap
        .values()
        .forEach(service -> builder.putServicePatches(service.fullName, service.createProto()));
    enumMap.values().forEach(e -> builder.putEnumPatches(e.fullName, e.createProto()));

    return builder.build();
  }

  static class FieldPatchContainer {
    List<RuleInfo> info = new ArrayList<>();
    List<OptionChange> optionChanges = new ArrayList<>();
    FieldChange patch;
    String name;
    int number;

    public void add(RuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    public FieldPatch createProto() {
      FieldPatch.Builder builder =
          FieldPatch.newBuilder()
              .setName(name)
              .setNumber(number)
              .addAllInfo(info)
              .addAllOptionChange(optionChanges);
      if (patch != null) {
        builder.setChange(patch);
      }
      return builder.build();
    }

    void addPatch(FieldChange patch) {
      this.patch = patch;
    }

    void addOptionChange(OptionChange optionChange) {
      this.optionChanges.add(optionChange);
    }
  }

  static class MessagePatchContainer {
    String packageName;
    String fullName;

    List<RuleInfo> info = new ArrayList<>();
    Map<String, FieldPatchContainer> fieldMap = new HashMap<>();
    Change patch;
    List<OptionChange> optionChanges = new ArrayList<>();

    public void add(Descriptors.FieldDescriptor field, RuleInfo ruleInfo) {
      FieldPatchContainer fieldResultContainer = getOrCreateFieldContainer(field);
      fieldResultContainer.add(ruleInfo);
    }

    void addPatch(Descriptors.FieldDescriptor field, FieldChange patch) {
      FieldPatchContainer fieldResultContainer = getOrCreateFieldContainer(field);
      fieldResultContainer.addPatch(patch);
    }

    private FieldPatchContainer getOrCreateFieldContainer(Descriptors.FieldDescriptor field) {
      FieldPatchContainer fieldResultContainer = fieldMap.get(field.getName());
      if (fieldResultContainer == null) {
        fieldResultContainer = new FieldPatchContainer();
        fieldResultContainer.name = field.getName();
        fieldResultContainer.number = field.getNumber();
        fieldMap.put(field.getName(), fieldResultContainer);
      }
      return fieldResultContainer;
    }

    MessagePatch createProto() {
      MessagePatch.Builder messageInfo = MessagePatch.newBuilder();
      messageInfo.setName(fullName);
      messageInfo.setPackage(packageName);
      if (patch != null) {
        messageInfo.setChange(patch);
      }
      fieldMap.values().forEach(field -> messageInfo.addFieldPatches(field.createProto()));
      messageInfo.addAllInfo(info);
      messageInfo.addAllOptionChange(optionChanges);
      return messageInfo.build();
    }

    void addResult(RuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    void setPatch(Change patch) {
      this.patch = patch;
    }

    void addOptionChange(OptionChange info) {
      optionChanges.add(info);
    }

    void addOptionChange(Descriptors.FieldDescriptor field, OptionChange optionChange) {
      FieldPatchContainer fieldResultContainer = getOrCreateFieldContainer(field);
      fieldResultContainer.addOptionChange(optionChange);
    }
  }

  class FilePatchContainer {
    String fullName;

    List<RuleInfo> info = new ArrayList<>();
    // Map<String, FieldPatchContainer> fieldMap = new HashMap<>();
    Change patch;
    List<OptionChange> optionChanges = new ArrayList<>();
    List<ImportChange> importChange = new ArrayList<>();

    void setPatch(Change patch) {
      this.patch = patch;
    }

    public FilePatch createProto() {

      FilePatch.Builder builder =
          FilePatch.newBuilder()
              .setFileName(fullName)
              .addAllInfo(info)
              .addAllOptionChange(optionChanges)
              .addAllImportChange(importChange);
      if (patch != null) {
        builder.setChange(patch);
      }
      return builder.build();
    }

    void addResult(RuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    void addOptionChange(OptionChange optionChange) {
      this.optionChanges.add(optionChange);
    }

    void addImportChange(ImportChange changeInfo) {
      this.importChange.add(changeInfo);
    }
  }

  class ServicePatchContainer {
    String packageName;
    String fullName;

    List<RuleInfo> info = new ArrayList<>();
    Map<String, MethodPatchContainer> methodMap = new HashMap<>();
    Change patch;

    public void add(Descriptors.MethodDescriptor method, RuleInfo ruleInfo) {
      MethodPatchContainer methoddResultContainer = getOrCreateMethodContainer(method);
      methoddResultContainer.add(ruleInfo);
    }

    public void addPatch(Descriptors.MethodDescriptor method, MethodChange patch) {
      MethodPatchContainer methodResultContainer = getOrCreateMethodContainer(method);
      methodResultContainer.addPatch(patch);
    }

    private MethodPatchContainer getOrCreateMethodContainer(Descriptors.MethodDescriptor method) {
      MethodPatchContainer methodResultContainer = methodMap.get(method.getName());
      if (methodResultContainer == null) {
        methodResultContainer = new MethodPatchContainer();
        methodResultContainer.fullName = method.getName();
        methodMap.put(method.getName(), methodResultContainer);
      }
      return methodResultContainer;
    }

    ServicePatch createProto() {
      ServicePatch.Builder serviceInfo = ServicePatch.newBuilder();
      serviceInfo.setPackage(packageName);
      serviceInfo.setName(fullName);
      if (patch != null) {
        serviceInfo.setChange(patch);
      }
      methodMap.values().forEach(method -> serviceInfo.addMethodPatches(method.createProto()));
      serviceInfo.addAllInfo(info);
      return serviceInfo.build();
    }

    void addResult(RuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    void setPatch(Change patch) {
      this.patch = patch;
    }
  }

  static class MethodPatchContainer {
    List<RuleInfo> info = new ArrayList<>();
    MethodChange patch;
    String fullName;

    public void add(RuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    public MethodPatch createProto() {
      MethodPatch.Builder builder = MethodPatch.newBuilder().setName(fullName).addAllInfo(info);
      if (patch != null) {
        builder.setChange(patch);
      }
      return builder.build();
    }

    void addPatch(MethodChange patch) {
      this.patch = patch;
    }
  }

  class EnumPatchContainer {
    String packageName;
    String fullName;

    List<RuleInfo> info = new ArrayList<>();
    Map<String, EnumValuePatchContainer> valueMap = new HashMap<>();
    Change patch;

    public void add(Descriptors.EnumValueDescriptor value, RuleInfo ruleInfo) {
      EnumValuePatchContainer methodResultContainer = getOrCreateValueContainer(value);
      methodResultContainer.add(ruleInfo);
    }

    public void addPatch(Descriptors.EnumValueDescriptor value, EnumValueChange patch) {
      EnumValuePatchContainer valueResultContainer = getOrCreateValueContainer(value);
      valueResultContainer.addPatch(patch);
    }

    private EnumValuePatchContainer getOrCreateValueContainer(
        Descriptors.EnumValueDescriptor value) {
      EnumValuePatchContainer valueResultContainer = valueMap.get(value.getName());
      if (valueResultContainer == null) {
        valueResultContainer = new EnumValuePatchContainer();
        valueResultContainer.fullName = value.getName();
        valueResultContainer.number = value.getNumber();
        valueMap.put(value.getName(), valueResultContainer);
      }
      return valueResultContainer;
    }

    EnumPatch createProto() {
      EnumPatch.Builder messageInfo = EnumPatch.newBuilder();
      messageInfo.setPackage(packageName);
      messageInfo.setName(fullName);
      if (patch != null) {
        messageInfo.setChange(patch);
      }
      valueMap.values().forEach(method -> messageInfo.addValuePatches(method.createProto()));
      messageInfo.addAllInfo(info);
      return messageInfo.build();
    }

    void addResult(RuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    void setPatch(Change patch) {
      this.patch = patch;
    }
  }

  static class EnumValuePatchContainer {
    List<RuleInfo> info = new ArrayList<>();
    EnumValueChange patch;
    String fullName;
    int number;

    public void add(RuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    public EnumValuePatch createProto() {
      EnumValuePatch.Builder builder =
          EnumValuePatch.newBuilder().setName(fullName).setNumber(number).addAllInfo(info);
      if (patch != null) {
        builder.setChange(patch);
      }
      return builder.build();
    }

    void addPatch(EnumValueChange patch) {
      this.patch = patch;
    }
  }
}
