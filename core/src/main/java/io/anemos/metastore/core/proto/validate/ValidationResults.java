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

  public List<LintRuleInfo> getInfo(String messageName, String fieldName) {
    List<LintRuleInfo> rules = new ArrayList<>();
    MessagePatchContainer messageResult = messageMap.get(messageName);
    if (messageResult != null) {
      FieldResultContainer fieldResultContainer = messageResult.fieldMap.get(fieldName);
      if (fieldResultContainer != null) {
        rules.addAll(fieldResultContainer.info);
      }
    }
    return rules;
  }

  private MessagePatchContainer getOrCreateMessage(String messageName) {
    MessagePatchContainer messageResult = messageMap.get(messageName);
    if (messageResult == null) {
      messageResult = new MessagePatchContainer();
      messageResult.fullName = messageName;
      messageMap.put(messageName, messageResult);
    }
    return messageResult;
  }

  private ServicePatchContainer getOrCreateService(String serviceName) {
    ServicePatchContainer serviceResult = serviceMap.get(serviceName);
    if (serviceResult == null) {
      serviceResult = new ServicePatchContainer();
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

  private EnumPatchContainer getOrCreateEnum(String fileName) {
    EnumPatchContainer enumResult = enumMap.get(fileName);
    if (enumResult == null) {
      enumResult = new EnumPatchContainer();
      enumResult.fullName = fileName;
      enumMap.put(fileName, enumResult);
    }
    return enumResult;
  }

  void addResult(Descriptors.FieldDescriptor fd, LintRuleInfo ruleInfo) {
    MessagePatchContainer messageResult = getOrCreateMessage(fd.getContainingType().getFullName());
    messageResult.add(fd, ruleInfo);
  }

  void addResult(Descriptors.MethodDescriptor md, LintRuleInfo ruleInfo) {
    ServicePatchContainer messageResult = getOrCreateService(md.getService().getFullName());
    messageResult.add(md, ruleInfo);
  }

  void addResult(Descriptors.Descriptor descriptor, LintRuleInfo ruleInfo) {
    MessagePatchContainer messageResult = getOrCreateMessage(descriptor.getFullName());
    messageResult.addResult(ruleInfo);
  }

  void addResult(Descriptors.ServiceDescriptor descriptor, LintRuleInfo ruleInfo) {
    ServicePatchContainer serviceResult = getOrCreateService(descriptor.getFullName());
    serviceResult.addResult(ruleInfo);
  }

  void addResult(Descriptors.FileDescriptor descriptor, LintRuleInfo ruleInfo) {
    FilePatchContainer fileResult = getOrCreateFile(descriptor.getFullName());
    fileResult.addResult(ruleInfo);
  }

  void setPatch(Descriptors.FieldDescriptor fd, FieldChangeInfo patch) {
    MessagePatchContainer resultContainer =
        getOrCreateMessage(fd.getContainingType().getFullName());
    resultContainer.addPatch(fd, patch);
  }

  void setPatch(Descriptors.MethodDescriptor fd, MethodChangeInfo patch) {
    ServicePatchContainer resultContainer = getOrCreateService(fd.getService().getFullName());
    resultContainer.addPatch(fd, patch);
  }

  void setPatch(Descriptors.EnumValueDescriptor fd, EnumValueChangeInfo patch) {
    EnumPatchContainer resultContainer = getOrCreateEnum(fd.getType().getFullName());
    resultContainer.addPatch(fd, patch);
  }

  void setPatch(Descriptors.Descriptor fd, ChangeInfo patch) {
    MessagePatchContainer resultContainer = getOrCreateMessage(fd.getFullName());
    resultContainer.setPatch(patch);
  }

  void setPatch(Descriptors.FileDescriptor fd, ChangeInfo patch) {
    FilePatchContainer resultContainer = getOrCreateFile(fd.getFullName());
    resultContainer.setPatch(patch);
  }

  void setPatch(Descriptors.EnumDescriptor fd, ChangeInfo patch) {
    EnumPatchContainer resultContainer = getOrCreateEnum(fd.getFullName());
    resultContainer.setPatch(patch);
  }

  void setPatch(Descriptors.ServiceDescriptor fd, ChangeInfo patch) {
    ServicePatchContainer serviceResult = getOrCreateService(fd.getFullName());
    serviceResult.setPatch(patch);
  }

  void addOptionChange(Descriptors.GenericDescriptor descriptor, OptionChangeInfo info) {
    if (descriptor instanceof Descriptors.FileDescriptor) {
      FilePatchContainer fileResultContainer = getOrCreateFile(descriptor.getFullName());
      fileResultContainer.addOptionChange(info);
    } else if (descriptor instanceof Descriptors.Descriptor) {
      MessagePatchContainer messageResult = getOrCreateMessage(descriptor.getFullName());
      messageResult.addOptionChange(info);
    } else if (descriptor instanceof Descriptors.FieldDescriptor) {
      Descriptors.FieldDescriptor fieldDescriptor = (Descriptors.FieldDescriptor) descriptor;
      MessagePatchContainer messageResult =
          getOrCreateMessage(fieldDescriptor.getContainingType().getFullName());
      messageResult.addOptionChange(fieldDescriptor, info);
    } else {
      // TODO
      throw new RuntimeException("Unimplemented option");
    }
  }

  void addImportChange(String fullName, ImportChangeInfo info) {
    FilePatchContainer fileResultContainer = getOrCreateFile(fullName);
    fileResultContainer.addImportChange(info);
  }

  public Patch getPatch() {
    Patch.Builder patchBuilder = Patch.newBuilder();
    fileMap.values().forEach(file -> patchBuilder.putFilePatches(file.fullName, file.getResult()));
    messageMap
        .values()
        .forEach(message -> patchBuilder.putMessagePatches(message.fullName, message.getResult()));
    serviceMap
        .values()
        .forEach(service -> patchBuilder.putServicePatches(service.fullName, service.getResult()));
    enumMap.values().forEach(e -> patchBuilder.putEnumPatches(e.fullName, e.getResult()));

    return patchBuilder.build();
  }

  static class FieldResultContainer {
    List<LintRuleInfo> info = new ArrayList<>();
    List<OptionChangeInfo> optionChangeInfos = new ArrayList<>();
    FieldChangeInfo patch;
    String name;
    int number;

    public void add(LintRuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    public FieldResult getResult() {
      FieldResult.Builder builder =
          FieldResult.newBuilder()
              .setName(name)
              .setNumber(number)
              .addAllInfo(info)
              .addAllOptionChange(optionChangeInfos);
      if (patch != null) {
        builder.setChange(patch);
      }
      return builder.build();
    }

    void addPatch(FieldChangeInfo patch) {
      this.patch = patch;
    }

    void addOptionChange(OptionChangeInfo optionChangeInfo) {
      this.optionChangeInfos.add(optionChangeInfo);
    }
  }

  static class MessagePatchContainer {
    String fullName;

    List<LintRuleInfo> info = new ArrayList<>();
    Map<String, FieldResultContainer> fieldMap = new HashMap<>();
    ChangeInfo patch;
    List<OptionChangeInfo> optionChangeInfos = new ArrayList<>();

    public void add(Descriptors.FieldDescriptor field, LintRuleInfo ruleInfo) {
      FieldResultContainer fieldResultContainer = getOrCreateFieldContainer(field);
      fieldResultContainer.add(ruleInfo);
    }

    void addPatch(Descriptors.FieldDescriptor field, FieldChangeInfo patch) {
      FieldResultContainer fieldResultContainer = getOrCreateFieldContainer(field);
      fieldResultContainer.addPatch(patch);
    }

    private FieldResultContainer getOrCreateFieldContainer(Descriptors.FieldDescriptor field) {
      FieldResultContainer fieldResultContainer = fieldMap.get(field.getName());
      if (fieldResultContainer == null) {
        fieldResultContainer = new FieldResultContainer();
        fieldResultContainer.name = field.getName();
        fieldResultContainer.number = field.getNumber();
        fieldMap.put(field.getName(), fieldResultContainer);
      }
      return fieldResultContainer;
    }

    MessagePatch getResult() {
      MessagePatch.Builder messageInfo = MessagePatch.newBuilder();
      messageInfo.setName(fullName);
      if (patch != null) {
        messageInfo.setChange(patch);
      }
      fieldMap.values().forEach(field -> messageInfo.addFieldResults(field.getResult()));
      messageInfo.addAllInfo(info);
      messageInfo.addAllOptionChange(optionChangeInfos);
      return messageInfo.build();
    }

    void addResult(LintRuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    void setPatch(ChangeInfo patch) {
      this.patch = patch;
    }

    void addOptionChange(OptionChangeInfo info) {
      optionChangeInfos.add(info);
    }

    void addOptionChange(Descriptors.FieldDescriptor field, OptionChangeInfo optionChangeInfo) {
      FieldResultContainer fieldResultContainer = getOrCreateFieldContainer(field);
      fieldResultContainer.addOptionChange(optionChangeInfo);
    }
  }

  class FilePatchContainer {
    String fullName;

    List<LintRuleInfo> info = new ArrayList<>();
    // Map<String, FieldResultContainer> fieldMap = new HashMap<>();
    ChangeInfo patch;
    List<OptionChangeInfo> optionChangeInfos = new ArrayList<>();
    List<ImportChangeInfo> importChangeInfo = new ArrayList<>();

    void setPatch(ChangeInfo patch) {
      this.patch = patch;
    }

    public FilePatch getResult() {

      FilePatch.Builder builder =
          FilePatch.newBuilder()
              .setFileName(fullName)
              .addAllInfo(info)
              .addAllOptionChange(optionChangeInfos)
              .addAllImportChange(importChangeInfo);
      if (patch != null) {
        builder.setChange(patch);
      }
      return builder.build();
    }

    void addResult(LintRuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    void addOptionChange(OptionChangeInfo optionChangeInfo) {
      this.optionChangeInfos.add(optionChangeInfo);
    }

    void addImportChange(ImportChangeInfo changeInfo) {
      this.importChangeInfo.add(changeInfo);
    }
  }

  class ServicePatchContainer {
    String fullName;

    List<LintRuleInfo> info = new ArrayList<>();
    Map<String, MethodResultContainer> methodMap = new HashMap<>();
    ChangeInfo patch;

    public void add(Descriptors.MethodDescriptor method, LintRuleInfo ruleInfo) {
      MethodResultContainer methoddResultContainer = getOrCreateMethodContainer(method);
      methoddResultContainer.add(ruleInfo);
    }

    public void addPatch(Descriptors.MethodDescriptor method, MethodChangeInfo patch) {
      MethodResultContainer methodResultContainer = getOrCreateMethodContainer(method);
      methodResultContainer.addPatch(patch);
    }

    private MethodResultContainer getOrCreateMethodContainer(Descriptors.MethodDescriptor method) {
      MethodResultContainer methodResultContainer = methodMap.get(method.getName());
      if (methodResultContainer == null) {
        methodResultContainer = new MethodResultContainer();
        methodResultContainer.fullName = method.getName();
        methodMap.put(method.getName(), methodResultContainer);
      }
      return methodResultContainer;
    }

    ServicePatch getResult() {
      ServicePatch.Builder messageInfo = ServicePatch.newBuilder();
      messageInfo.setName(fullName);
      if (patch != null) {
        messageInfo.setChange(patch);
      }
      methodMap.values().forEach(method -> messageInfo.addMethodResults(method.getResult()));
      messageInfo.addAllInfo(info);
      return messageInfo.build();
    }

    void addResult(LintRuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    void setPatch(ChangeInfo patch) {
      this.patch = patch;
    }
  }

  static class MethodResultContainer {
    List<LintRuleInfo> info = new ArrayList<>();
    MethodChangeInfo patch;
    String fullName;

    public void add(LintRuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    public MethodResult getResult() {
      MethodResult.Builder builder = MethodResult.newBuilder().setName(fullName).addAllInfo(info);
      if (patch != null) {
        builder.setChange(patch);
      }
      return builder.build();
    }

    void addPatch(MethodChangeInfo patch) {
      this.patch = patch;
    }
  }

  class EnumPatchContainer {
    String fullName;

    List<LintRuleInfo> info = new ArrayList<>();
    Map<String, EnumValueResultContainer> valueMap = new HashMap<>();
    ChangeInfo patch;

    public void add(Descriptors.EnumValueDescriptor value, LintRuleInfo ruleInfo) {
      EnumValueResultContainer methodResultContainer = getOrCreateValueContainer(value);
      methodResultContainer.add(ruleInfo);
    }

    public void addPatch(Descriptors.EnumValueDescriptor value, EnumValueChangeInfo patch) {
      EnumValueResultContainer valueResultContainer = getOrCreateValueContainer(value);
      valueResultContainer.addPatch(patch);
    }

    private EnumValueResultContainer getOrCreateValueContainer(
        Descriptors.EnumValueDescriptor value) {
      EnumValueResultContainer valueResultContainer = valueMap.get(value.getName());
      if (valueResultContainer == null) {
        valueResultContainer = new EnumValueResultContainer();
        valueResultContainer.fullName = value.getName();
        valueResultContainer.number = value.getNumber();
        valueMap.put(value.getName(), valueResultContainer);
      }
      return valueResultContainer;
    }

    EnumPatch getResult() {
      EnumPatch.Builder messageInfo = EnumPatch.newBuilder();
      messageInfo.setName(fullName);
      if (patch != null) {
        messageInfo.setChange(patch);
      }
      valueMap.values().forEach(method -> messageInfo.addValueResults(method.getResult()));
      messageInfo.addAllInfo(info);
      return messageInfo.build();
    }

    void addResult(LintRuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    void setPatch(ChangeInfo patch) {
      this.patch = patch;
    }
  }

  static class EnumValueResultContainer {
    List<LintRuleInfo> info = new ArrayList<>();
    EnumValueChangeInfo patch;
    String fullName;
    int number;

    public void add(LintRuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    public EnumValueResult getResult() {
      EnumValueResult.Builder builder =
          EnumValueResult.newBuilder().setName(fullName).setNumber(number).addAllInfo(info);
      if (patch != null) {
        builder.setChange(patch);
      }
      return builder.build();
    }

    void addPatch(EnumValueChangeInfo patch) {
      this.patch = patch;
    }
  }
}
