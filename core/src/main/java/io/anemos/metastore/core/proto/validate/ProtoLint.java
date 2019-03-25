package io.anemos.metastore.core.proto.validate;

import com.google.protobuf.Descriptors;
import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.v1alpha1.LintRule;
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
        fileDescriptor.getServices().forEach(service -> lintOnService(service.getFullName()));
        fileDescriptor.getEnumTypes().forEach(enu -> lintOnEnum(enu.getFullName()));
    }

    public void lintOnMessage(String fullName) {
        lintMessage(proto.getDescriptorByName(fullName));
    }

    public void lintOnService(Descriptors.ServiceDescriptor service) {
        String name = service.getName();
        if (!isPascalCase(name)) {
            results.addResult(service, RuleInfo.newBuilder()
                    .setLintRule(LintRule.LINT_SERVICE_NAME_SHOULD_BE_PASCAL)
                    .setCode(String.format("L%d/00", LintRule.LINT_SERVICE_NAME_SHOULD_BE_PASCAL_VALUE))
                    .build()
            );
        }

        service.getMethods().forEach(m -> lintMethod(m));
    }

    public void lintOnService(String fullName) {
        lintOnService(proto.getServiceDescriptorByName(fullName));
    }

    public void lintOnEnum(String fullName) {
        lintOnEnum(proto.getEnumDescriptorByName(fullName));
    }

    private void lintOnEnum(Descriptors.EnumDescriptor enu) {

    }

    public void lintOnPackagePrefix(String packagePrefix) {
        List<Descriptors.FileDescriptor> fdRef = proto.getFileDescriptorsByPackagePrefix(packagePrefix);
        lintFiles(fdRef);
    }

    private void lintFiles(List<Descriptors.FileDescriptor> f_ref) {
        f_ref.forEach(v -> lintOnFileName(v.getName()));
    }

    private void lintMessage(Descriptors.Descriptor dp) {
        String name = dp.getName();
        if (!isPascalCase(name)) {
            results.addResult(dp, RuleInfo.newBuilder()
                    .setLintRule(LintRule.LINT_MESSAGE_NAME_SHOULD_BE_PASCAL)
                    .setCode(String.format("L%d/00", LintRule.LINT_MESSAGE_NAME_SHOULD_BE_PASCAL_VALUE))
                    .build()
            );
        }

        dp.getFields().forEach(fd -> lintField(fd));
    }

    private void lintMethod(Descriptors.MethodDescriptor md) {
        if (!md.getInputType().getFullName().endsWith("Request")) {
            results.addResult(md, RuleInfo.newBuilder()
                    .setLintRule(LintRule.LINT_METHOD_ITYPE_END_WITH_REQUEST)
                    .setCode(String.format("L%d/00", LintRule.LINT_METHOD_ITYPE_END_WITH_REQUEST_VALUE))
                    .build());
        }
        if (!md.getOutputType().getFullName().endsWith("Response")) {
            results.addResult(md, RuleInfo.newBuilder()
                    .setLintRule(LintRule.LINT_METHOD_RTYPE_END_WITH_RESPONSE)
                    .setCode(String.format("L%d/00", LintRule.LINT_METHOD_RTYPE_END_WITH_RESPONSE_VALUE))
                    .build());
        }
    }

    private void lintField(Descriptors.FieldDescriptor fd) {
        String name = fd.getName();
        String suffix = isSnakeCase(name);
        if (suffix != null) {
            results.addResult(fd, RuleInfo.newBuilder()
                    .setLintRule(LintRule.LINT_FIELD_NAME_SHOULD_BE_SNAKE)
                    .setCode(String.format("L%d/%s", LintRule.LINT_FIELD_NAME_SHOULD_BE_SNAKE_VALUE, suffix))
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

    private boolean isPascalCase(String fieldName) {
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
