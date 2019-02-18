package io.anemos.metastore.core.proto.validate;

import com.google.protobuf.Descriptors;
import io.anemos.metastore.v1alpha1.Report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationResults {
    Map<String, MessageResultContainer> messageMap = new HashMap<>();

    public List<Report.RuleInfo> getInfo(String messageName, String fieldName) {
        List<Report.RuleInfo> rules = new ArrayList<>();
        MessageResultContainer messageResult = messageMap.get(messageName);
        if (messageResult != null) {
            FieldResultContainer fieldResultContainer = messageResult.fieldMap.get(fieldName);
            if (fieldResultContainer != null) {
                rules.addAll(fieldResultContainer.info);
            }
        }
        return rules;
    }

    MessageResultContainer getOrCreateMessage(String messageName) {
        MessageResultContainer messageResult = messageMap.get(messageName);
        if (messageResult == null) {
            messageResult = new MessageResultContainer();
            messageResult.fullName = messageName;
            messageMap.put(messageName, messageResult);
        }
        return messageResult;
    }

    void addResult(Descriptors.FieldDescriptor fd, Report.RuleInfo ruleInfo) {
        MessageResultContainer messageResult = getOrCreateMessage(fd.getContainingType().getFullName());
        messageResult.add(fd, ruleInfo);
    }

    void addResult(Descriptors.Descriptor descriptor, Report.RuleInfo ruleInfo) {
        MessageResultContainer messageResult = getOrCreateMessage(descriptor.getFullName());
        messageResult.addResult(ruleInfo);
    }

    void setPatch(Descriptors.FieldDescriptor fd, Report.DeltaPatch patch) {
        MessageResultContainer messageResult = getOrCreateMessage(fd.getContainingType().getFullName());
        messageResult.addPatch(fd, patch);
    }


    public Report.FileResult getResult() {
        Report.FileResult.Builder fileResult = Report.FileResult.newBuilder();
        messageMap.values().forEach(message -> {
            fileResult.addMessageResults(message.getResult());

        });
        return fileResult.build();
    }

    class FieldResultContainer {
        List<Report.RuleInfo> info = new ArrayList();
        Report.DeltaPatch patch;
        String name;
        int number;

        public void add(Report.RuleInfo ruleInfo) {
            info.add(ruleInfo);
        }

        public Report.FieldResult getResult() {
            Report.FieldResult.Builder builder = Report.FieldResult.newBuilder()
                    .setName(name)
                    .addAllInfo(info);
            if (patch != null) {
                builder.setDelta(patch);
            }
            return builder.build();
        }

        public void addPatch(Report.DeltaPatch patch) {
            this.patch = patch;
        }
    }

    class MessageResultContainer {
        String fullName;

        List<Report.RuleInfo> info = new ArrayList<>();
        Map<String, FieldResultContainer> fieldMap = new HashMap<>();
        Report.DeltaPatch patch;

        public void add(Descriptors.FieldDescriptor field, Report.RuleInfo ruleInfo) {
            FieldResultContainer fieldResultContainer = getOrCreateFieldContainer(field);
            fieldResultContainer.add(ruleInfo);
        }

        public void addPatch(Descriptors.FieldDescriptor field, Report.DeltaPatch patch) {
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

        Report.MessageResult getResult() {
            Report.MessageResult.Builder messageInfo = Report.MessageResult.newBuilder();
            messageInfo.setName(fullName);
            fieldMap.values().forEach(field -> messageInfo.addFieldResults(field.getResult()));
            messageInfo.addAllInfo(info);
            return messageInfo.build();
        }

        public void addResult(Report.RuleInfo ruleInfo) {
            info.add(ruleInfo);
        }

        public void addPatch(Report.DeltaPatch patch) {
            this.patch = patch;
        }

    }
}
