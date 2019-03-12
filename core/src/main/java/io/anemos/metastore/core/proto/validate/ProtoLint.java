package io.anemos.metastore.core.proto.validate;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.v1alpha1.LintRule;
import io.anemos.metastore.v1alpha1.Report;
import io.anemos.metastore.v1alpha1.RuleInfo;

import java.util.List;

public class ProtoLint {

    private ProtoDescriptor proto;
    private ValidationResults results;


    public ProtoLint(ProtoDescriptor fd_ref, ValidationResults results) {
        this.proto = fd_ref;
        this.results = results;
    }

    public void lint() {
        proto.getFileNames().forEach(fileName -> lintOnFileName(fileName));
    }

    public void lintOnFileName(String fileName) {
        Descriptors.FileDescriptor fileDescriptor = proto.getFileDescriptorByFileName(fileName);
        fileDescriptor.getMessageTypes().forEach(message -> lintOnMessage(message.getFullName()));
    }

    public void lintOnMessage(String fullName) {
        Descriptors.Descriptor descriptor = proto.getDescriptorByName(fullName);
        diffMessageType(descriptor);
//        diffMessageTypes(proto.getMessageTypeList());
//        diffEnumTypes(proto.getEnumTypeList());
//        diffServices(proto.getServiceList());
    }

    private void diffServices(List<DescriptorProtos.ServiceDescriptorProto> s_ref) {

    }

    private void diffEnumTypes(List<DescriptorProtos.EnumDescriptorProto> e_ref) {

    }


    private void diffMessageTypes(List<DescriptorProtos.DescriptorProto> messages) {
//        messages.forEach(dp -> {
//            diffMessageType(dp);
//        });
    }

    private void diffMessageType(Descriptors.Descriptor dp) {
        String name = dp.getName();
        if (!isCamelCase(name)) {
            results.addResult(dp, RuleInfo.newBuilder()
                    .setLintRule(LintRule.LINT_MESSAGE_NAME)
                    .build()
            );
        }

        dp.getFields().forEach(fd -> {
            diffField(fd);
        });
    }

    private void diffField(Descriptors.FieldDescriptor fd) {
        String name = fd.getName();
        String suffix = isSnakeCase(name);
        if (suffix != null) {
            results.addResult(fd, RuleInfo.newBuilder()
                    .setLintRule(LintRule.LINT_FIELD_NAME)
                    .build());
        }
    }

    private String isSnakeCase(String fieldName) {
        if (!Character.isLowerCase(fieldName.charAt(0))) {
            return "01";
        }

        boolean wasUnderscore = false;
        boolean wasNumber = false;
        for (int ix = 1; ix < fieldName.length() - 2; ix++) {
            char c = fieldName.charAt(ix);
            if (c == '_') {
                if (wasUnderscore) {
                    // don't double underscore
                    return "03";
                }
                wasNumber = false;
                wasUnderscore = true;
            } else if (Character.isLowerCase(c)) {
                if (wasNumber) {
                    return "04";
                }
                wasNumber = false;
                wasUnderscore = false;
            } else if (Character.isDigit(c)) {
                wasNumber = true;
                wasUnderscore = false;
            } else {
                return "02";
            }
        }

        char c = fieldName.charAt(fieldName.length() - 1);
        if (!Character.isLowerCase(c) && !Character.isDigit(c)) {
            return "05";
        }
        return null;
    }

    private boolean isCamelCase(String fieldName) {
        if (!Character.isUpperCase(fieldName.charAt(0))) {
            return false;
        }

        boolean wasNumber = false;
        for (int ix = 1; ix < fieldName.length() - 1; ix++) {
            char c = fieldName.charAt(ix);
            if (Character.isAlphabetic(c)) {
                if (wasNumber && Character.isLowerCase(c)) {
                    return false;
                }
                wasNumber = false;
            } else if (Character.isDigit(c)) {
                wasNumber = true;
            } else {
                return false;
            }
        }

        return true;
    }

}
