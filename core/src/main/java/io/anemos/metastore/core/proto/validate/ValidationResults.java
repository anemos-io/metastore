package io.anemos.metastore.core.proto.validate;

import com.google.protobuf.Descriptors;
import io.anemos.metastore.v1alpha1.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationResults {
  private Map<String, FileResultContainer> fileMap = new HashMap<>();
  private Map<String, MessageResultContainer> messageMap = new HashMap<>();
  private Map<String, EnumResultContainer> enumMap = new HashMap<>();
  private Map<String, ServiceResultContainer> serviceMap = new HashMap<>();

  public List<RuleInfo> getInfo(String messageName, String fieldName) {
    List<RuleInfo> rules = new ArrayList<>();
    MessageResultContainer messageResult = messageMap.get(messageName);
    if (messageResult != null) {
      FieldResultContainer fieldResultContainer = messageResult.fieldMap.get(fieldName);
      if (fieldResultContainer != null) {
        rules.addAll(fieldResultContainer.info);
      }
    }
    return rules;
  }

  private MessageResultContainer getOrCreateMessage(String messageName) {
    MessageResultContainer messageResult = messageMap.get(messageName);
    if (messageResult == null) {
      messageResult = new MessageResultContainer();
      messageResult.fullName = messageName;
      messageMap.put(messageName, messageResult);
    }
    return messageResult;
  }

  private ServiceResultContainer getOrCreateService(String serviceName) {
    ServiceResultContainer serviceResult = serviceMap.get(serviceName);
    if (serviceResult == null) {
      serviceResult = new ServiceResultContainer();
      serviceResult.fullName = serviceName;
      serviceMap.put(serviceName, serviceResult);
    }
    return serviceResult;
  }

  private FileResultContainer getOrCreateFile(String fileName) {
    FileResultContainer fileResult = fileMap.get(fileName);
    if (fileResult == null) {
      fileResult = new FileResultContainer();
      fileResult.fullName = fileName;
      fileMap.put(fileName, fileResult);
    }
    return fileResult;
  }

  private EnumResultContainer getOrCreateEnum(String fileName) {
    EnumResultContainer enumResult = enumMap.get(fileName);
    if (enumResult == null) {
      enumResult = new EnumResultContainer();
      enumResult.fullName = fileName;
      enumMap.put(fileName, enumResult);
    }
    return enumResult;
  }

  void addResult(Descriptors.FieldDescriptor fd, RuleInfo ruleInfo) {
    MessageResultContainer messageResult = getOrCreateMessage(fd.getContainingType().getFullName());
    messageResult.add(fd, ruleInfo);
  }

  void addResult(Descriptors.MethodDescriptor md, RuleInfo ruleInfo) {
    ServiceResultContainer messageResult = getOrCreateService(md.getService().getFullName());
    messageResult.add(md, ruleInfo);
  }

  void addResult(Descriptors.Descriptor descriptor, RuleInfo ruleInfo) {
    MessageResultContainer messageResult = getOrCreateMessage(descriptor.getFullName());
    messageResult.addResult(ruleInfo);
  }

  void addResult(Descriptors.ServiceDescriptor descriptor, RuleInfo ruleInfo) {
    ServiceResultContainer serviceResult = getOrCreateService(descriptor.getFullName());
    serviceResult.addResult(ruleInfo);
  }

  void addResult(Descriptors.FileDescriptor descriptor, RuleInfo ruleInfo) {
    FileResultContainer fileResult = getOrCreateFile(descriptor.getFullName());
    fileResult.addResult(ruleInfo);
  }

  void setPatch(Descriptors.FieldDescriptor fd, FieldChangeInfo patch) {
    MessageResultContainer resultContainer =
        getOrCreateMessage(fd.getContainingType().getFullName());
    resultContainer.addPatch(fd, patch);
  }

  void setPatch(Descriptors.MethodDescriptor fd, MethodChangeInfo patch) {
    ServiceResultContainer resultContainer = getOrCreateService(fd.getService().getFullName());
    resultContainer.addPatch(fd, patch);
  }

  void setPatch(Descriptors.EnumValueDescriptor fd, EnumValueChangeInfo patch) {
    EnumResultContainer resultContainer = getOrCreateEnum(fd.getType().getFullName());
    resultContainer.addPatch(fd, patch);
  }

  void setPatch(Descriptors.Descriptor fd, ChangeInfo patch) {
    MessageResultContainer resultContainer = getOrCreateMessage(fd.getFullName());
    resultContainer.setPatch(patch);
  }

  void setPatch(Descriptors.FileDescriptor fd, ChangeInfo patch) {
    FileResultContainer resultContainer = getOrCreateFile(fd.getFullName());
    resultContainer.setPatch(patch);
  }

  void setPatch(Descriptors.EnumDescriptor fd, ChangeInfo patch) {
    EnumResultContainer resultContainer = getOrCreateEnum(fd.getFullName());
    resultContainer.setPatch(patch);
  }

  void setPatch(Descriptors.ServiceDescriptor fd, ChangeInfo patch) {
    ServiceResultContainer serviceResult = getOrCreateService(fd.getFullName());
    serviceResult.setPatch(patch);
  }

  void addOptionChange(Descriptors.GenericDescriptor descriptor, OptionChangeInfo info) {
    if (descriptor instanceof Descriptors.FileDescriptor) {
      FileResultContainer fileResultContainer = getOrCreateFile(descriptor.getFullName());
      fileResultContainer.addOptionChange(info);
    } else if (descriptor instanceof Descriptors.Descriptor) {
      MessageResultContainer messageResult = getOrCreateMessage(descriptor.getFullName());
      messageResult.addOptionChange(info);
    } else if (descriptor instanceof Descriptors.FieldDescriptor) {
      Descriptors.FieldDescriptor fieldDescriptor = (Descriptors.FieldDescriptor) descriptor;
      MessageResultContainer messageResult =
          getOrCreateMessage(fieldDescriptor.getContainingType().getFullName());
      messageResult.addOptionChange(fieldDescriptor, info);
    } else {
      // TODO
      throw new RuntimeException("Unimplemented option");
    }
  }

  void addImportChange(String fullName, ImportChangeInfo info) {
    FileResultContainer fileResultContainer = getOrCreateFile(fullName);
    fileResultContainer.addImportChange(info);
  }

  public Report getReport() {
    Report.Builder builder = Report.newBuilder();
    fileMap.values().forEach(file -> builder.putFileResults(file.fullName, file.getResult()));
    messageMap
        .values()
        .forEach(message -> builder.putMessageResults(message.fullName, message.getResult()));
    serviceMap
        .values()
        .forEach(service -> builder.putServiceResults(service.fullName, service.getResult()));
    enumMap.values().forEach(e -> builder.putEnumResults(e.fullName, e.getResult()));

    return builder.build();
  }

  static class FieldResultContainer {
    List<RuleInfo> info = new ArrayList<>();
    List<OptionChangeInfo> optionChangeInfos = new ArrayList<>();
    FieldChangeInfo patch;
    String name;
    int number;

    public void add(RuleInfo ruleInfo) {
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

  static class MessageResultContainer {
    String fullName;

    List<RuleInfo> info = new ArrayList<>();
    Map<String, FieldResultContainer> fieldMap = new HashMap<>();
    ChangeInfo patch;
    List<OptionChangeInfo> optionChangeInfos = new ArrayList<>();

    public void add(Descriptors.FieldDescriptor field, RuleInfo ruleInfo) {
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

    MessageResult getResult() {
      MessageResult.Builder messageInfo = MessageResult.newBuilder();
      messageInfo.setName(fullName);
      if (patch != null) {
        messageInfo.setChange(patch);
      }
      fieldMap.values().forEach(field -> messageInfo.addFieldResults(field.getResult()));
      messageInfo.addAllInfo(info);
      messageInfo.addAllOptionChange(optionChangeInfos);
      return messageInfo.build();
    }

    void addResult(RuleInfo ruleInfo) {
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

  class FileResultContainer {
    String fullName;

    List<RuleInfo> info = new ArrayList<>();
    // Map<String, FieldResultContainer> fieldMap = new HashMap<>();
    ChangeInfo patch;
    List<OptionChangeInfo> optionChangeInfos = new ArrayList<>();
    List<ImportChangeInfo> importChangeInfo = new ArrayList<>();

    void setPatch(ChangeInfo patch) {
      this.patch = patch;
    }

    public FileResult getResult() {

      FileResult.Builder builder =
          FileResult.newBuilder()
              .setFileName(fullName)
              .addAllInfo(info)
              .addAllOptionChange(optionChangeInfos)
              .addAllImportChange(importChangeInfo);
      if (patch != null) {
        builder.setChange(patch);
      }
      return builder.build();
    }

    void addResult(RuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    void addOptionChange(OptionChangeInfo optionChangeInfo) {
      this.optionChangeInfos.add(optionChangeInfo);
    }

    void addImportChange(ImportChangeInfo changeInfo) {
      this.importChangeInfo.add(changeInfo);
    }
  }

  class ServiceResultContainer {
    String fullName;

    List<RuleInfo> info = new ArrayList<>();
    Map<String, MethodResultContainer> methodMap = new HashMap<>();
    ChangeInfo patch;

    public void add(Descriptors.MethodDescriptor method, RuleInfo ruleInfo) {
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

    ServiceResult getResult() {
      ServiceResult.Builder messageInfo = ServiceResult.newBuilder();
      messageInfo.setName(fullName);
      if (patch != null) {
        messageInfo.setChange(patch);
      }
      methodMap.values().forEach(method -> messageInfo.addMethodResults(method.getResult()));
      messageInfo.addAllInfo(info);
      return messageInfo.build();
    }

    void addResult(RuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    void setPatch(ChangeInfo patch) {
      this.patch = patch;
    }
  }

  static class MethodResultContainer {
    List<RuleInfo> info = new ArrayList<>();
    MethodChangeInfo patch;
    String fullName;

    public void add(RuleInfo ruleInfo) {
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

  class EnumResultContainer {
    String fullName;

    List<RuleInfo> info = new ArrayList<>();
    Map<String, EnumValueResultContainer> valueMap = new HashMap<>();
    ChangeInfo patch;

    public void add(Descriptors.EnumValueDescriptor value, RuleInfo ruleInfo) {
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

    EnumResult getResult() {
      EnumResult.Builder messageInfo = EnumResult.newBuilder();
      messageInfo.setName(fullName);
      if (patch != null) {
        messageInfo.setChange(patch);
      }
      valueMap.values().forEach(method -> messageInfo.addValueResults(method.getResult()));
      messageInfo.addAllInfo(info);
      return messageInfo.build();
    }

    void addResult(RuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    void setPatch(ChangeInfo patch) {
      this.patch = patch;
    }
  }

  static class EnumValueResultContainer {
    List<RuleInfo> info = new ArrayList<>();
    EnumValueChangeInfo patch;
    String fullName;
    int number;

    public void add(RuleInfo ruleInfo) {
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
