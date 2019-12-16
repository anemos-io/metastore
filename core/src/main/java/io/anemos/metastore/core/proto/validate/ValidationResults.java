package io.anemos.metastore.core.proto.validate;

import com.google.protobuf.Descriptors;
import io.anemos.metastore.v1alpha1.*;
import sun.nio.ch.sctp.ResultContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationResults {
  private Map<String, FileResultContainer> fileMap = new HashMap<>();
  private Map<String, MessageResultContainer> messageMap = new HashMap<>();
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

  private EnumResultContainer getOrCreateFile(String fileName) {
    FileResultContainer fileResult = fileMap.get(fileName);
    if (fileResult == null) {
      fileResult = new FileResultContainer();
      fileResult.fullName = fileName;
      fileMap.put(fileName, fileResult);
    }
    return fileResult;
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
    MessageResultContainer messageResult = getOrCreateMessage(fd.getContainingType().getFullName());
    messageResult.addPatch(fd, patch);
  }

  void setPatch(Descriptors.Descriptor fd, ChangeInfo patch) {
    MessageResultContainer messageResult = getOrCreateMessage(fd.getFullName());
    messageResult.setPatch(patch);
  }

  void setPatch(Descriptors.FileDescriptor fd, ChangeInfo patch) {
    FileResultContainer fileResult = getOrCreateFile(fd.getFullName());
    fileResult.setPatch(patch);
  }

  void setPatch(Descriptors.EnumDescriptor fd, ChangeInfo patch) {
    throw new RuntimeException("Unimplemented patch");
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
    fileMap
        .values()
        .forEach(
            file -> {
              builder.putFileResults(file.fullName, file.getResult());
            });
    messageMap
        .values()
        .forEach(
            message -> {
              builder.putMessageResults(message.fullName, message.getResult());
            });
    serviceMap
        .values()
        .forEach(
            service -> {
              builder.putServiceResults(service.fullName, service.getResult());
            });

    return builder.build();
  }

  class FieldResultContainer {
    List<RuleInfo> info = new ArrayList();
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

    public void addPatch(FieldChangeInfo patch) {
      this.patch = patch;
    }

    public void addOptionChange(OptionChangeInfo optionChangeInfo) {
      this.optionChangeInfos.add(optionChangeInfo);
    }
  }

  class MessageResultContainer {
    String fullName;

    List<RuleInfo> info = new ArrayList<>();
    Map<String, FieldResultContainer> fieldMap = new HashMap<>();
    ChangeInfo patch;
    List<OptionChangeInfo> optionChangeInfos = new ArrayList<>();

    public void add(Descriptors.FieldDescriptor field, RuleInfo ruleInfo) {
      FieldResultContainer fieldResultContainer = getOrCreateFieldContainer(field);
      fieldResultContainer.add(ruleInfo);
    }

    public void addPatch(Descriptors.FieldDescriptor field, FieldChangeInfo patch) {
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

    public void addResult(RuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    public void setPatch(ChangeInfo patch) {
      this.patch = patch;
    }

    public void addOptionChange(OptionChangeInfo info) {
      optionChangeInfos.add(info);
    }

    public void addOptionChange(
        Descriptors.FieldDescriptor field, OptionChangeInfo optionChangeInfo) {
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

    public void setPatch(ChangeInfo patch) {
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

    public void addResult(RuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    public void addOptionChange(OptionChangeInfo optionChangeInfo) {
      this.optionChangeInfos.add(optionChangeInfo);
    }

    public void addImportChange(ImportChangeInfo changeInfo) {
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

    public void addPatch(Descriptors.MethodDescriptor method, ChangeInfo patch) {
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

    public void addResult(RuleInfo ruleInfo) {
      info.add(ruleInfo);
    }

    public void setPatch(ChangeInfo patch) {
      this.patch = patch;
    }
  }

  class MethodResultContainer {
    List<RuleInfo> info = new ArrayList();
    ChangeInfo patch;
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

    public void addPatch(ChangeInfo patch) {
      this.patch = patch;
    }
  }
}
