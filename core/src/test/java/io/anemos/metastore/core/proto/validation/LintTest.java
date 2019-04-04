package io.anemos.metastore.core.proto.validation;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Service;
import io.anemos.metastore.core.Lint;
import io.anemos.metastore.core.proto.*;
import io.anemos.metastore.core.proto.validate.ProtoLint;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.invalid.Invalid;
import io.anemos.metastore.v1alpha1.*;
import org.junit.Assert;
import org.junit.ComparisonFailure;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class LintTest {

    @Test
    public void fieldOkSnakeCase() throws IOException {
        Report result = lintMessage(Lint.LintFieldNamesGood.getDescriptor());
        Assert.assertEquals(0, result.getMessageResultsCount());
    }

    @Test
    public void fieldNokCamel() throws IOException {
        Report result = lintMessage(Lint.LintFieldNamesBad.getDescriptor());
        Assert.assertEquals(1, result.getMessageResultsCount());

        MessageResult mr = result.getMessageResultsOrThrow("anemos.metastore.core.LintFieldNamesBad");
        assertOnField(mr, 1, LintRule.LINT_FIELD_NAME_SHOULD_BE_SNAKE, "L20001/02");
        assertOnField(mr, 2, LintRule.LINT_FIELD_NAME_SHOULD_BE_SNAKE, "L20001/01");
        assertOnField(mr, 3, LintRule.LINT_FIELD_NAME_SHOULD_BE_SNAKE, "L20001/05");
    }

    @Test
    public void messageLowerCase() throws IOException {
        Report result = lintMessage(Lint.lintmessagelowercase.getDescriptor());
        Assert.assertEquals(1, result.getMessageResultsCount());

        MessageResult mr = result.getMessageResultsOrThrow("anemos.metastore.core.lintmessagelowercase");
        assertOnMessage(mr, LintRule.LINT_MESSAGE_NAME_SHOULD_BE_PASCAL, "L10001/00");
    }

    @Test
    public void messageCamelCase() throws IOException {
        Report result = lintMessage(Lint.lint_message_camelcase.getDescriptor());
        Assert.assertEquals(1, result.getMessageResultsCount());

        MessageResult mr = result.getMessageResultsOrThrow("anemos.metastore.core.lint_message_camelcase");
        assertOnMessage(mr, LintRule.LINT_MESSAGE_NAME_SHOULD_BE_PASCAL, "L10001/00");
    }

    @Test
    public void methodInputAndReturnTypeNoRR() throws IOException {
        Report result = lintService(Lint.LintFieldNamesGood.getDescriptor(), "anemos.metastore.core.MethodService");

        ServiceResult mr = result.getServiceResultsOrThrow("anemos.metastore.core.MethodService");
        Assert.assertEquals(2, getInfoForMethod(mr, "MethodEmpty").size());
        assertOnMethod(mr, "MethodEmpty", LintRule.LINT_METHOD_RTYPE_END_WITH_RESPONSE, "L50003/00");
        assertOnMethod(mr, "MethodEmpty", LintRule.LINT_METHOD_ITYPE_END_WITH_REQUEST, "L50002/00");
    }

    @Test
    public void methodInputTypeNoR() throws IOException {
        Report result = lintService(Lint.LintFieldNamesGood.getDescriptor(), "anemos.metastore.core.MethodService");

        ServiceResult mr = result.getServiceResultsOrThrow("anemos.metastore.core.MethodService");
        Assert.assertEquals(1, getInfoForMethod(mr, "MethodEmptyI").size());
        assertOnMethod(mr, "MethodEmptyI", LintRule.LINT_METHOD_ITYPE_END_WITH_REQUEST, "L50002/00");
    }

    @Test
    public void methodReturnTypeNoR() throws IOException {
        Report result = lintService(Lint.LintFieldNamesGood.getDescriptor(), "anemos.metastore.core.MethodService");

        ServiceResult mr = result.getServiceResultsOrThrow("anemos.metastore.core.MethodService");
        Assert.assertEquals(1, getInfoForMethod(mr, "MethodEmptyR").size());
        assertOnMethod(mr, "MethodEmptyR", LintRule.LINT_METHOD_RTYPE_END_WITH_RESPONSE, "L50003/00");
    }

    @Test
    public void methodOk() throws IOException {
        Report result = lintService(Lint.LintFieldNamesGood.getDescriptor(), "anemos.metastore.core.MethodService");

        ServiceResult mr = result.getServiceResultsOrThrow("anemos.metastore.core.MethodService");
        Assert.assertNull(getInfoForMethod(mr, "MethodOk"));
    }


    private Report lintMessage(Descriptors.Descriptor d) throws IOException {
        ProtoDescriptor pd = new ProtoDescriptor(d);
        String message = d.getFullName();

        ValidationResults results = new ValidationResults();
        ProtoLint lint = new ProtoLint(pd, results);
        lint.lintOnMessage(message);
        return results.getReport();
    }

    private Report lintService(Descriptors.Descriptor ref, String name) throws IOException {
        ProtoDescriptor pd = new ProtoDescriptor(ref);

        ValidationResults results = new ValidationResults();
        ProtoLint lint = new ProtoLint(pd, results);
        lint.lintOnService(name);

        return results.getReport();
    }

    private List<RuleInfo> getInfoForField(MessageResult mr, int fieldNumber) {
        for (FieldResult fieldResult : mr.getFieldResultsList()) {
            if (fieldResult.getNumber() == fieldNumber) {
                return fieldResult.getInfoList();
            }
        }
        Assert.assertNotNull(null);
        return null;
    }

    private List<RuleInfo> getInfoForMethod(ServiceResult mr, String methodName) {
        for (MethodResult methodResult : mr.getMethodResultsList()) {
            if (methodResult.getName().equals(methodName)) {
                return methodResult.getInfoList();
            }
        }
        return null;
    }


    private void assertOnMethod(ServiceResult mr, String methodName, LintRule expecredRule, String expectedCode) {
        List<RuleInfo> infoForField = getInfoForMethod(mr, methodName);
        String code = null;
        LintRule rule = null;
        for (RuleInfo ruleInfo : infoForField) {
            if (ruleInfo.getCode().equals(expectedCode)
                    && ruleInfo.getLintRule().equals(expecredRule)) {
                return;
            }
            code = ruleInfo.getCode();
            rule = ruleInfo.getLintRule();
        }
        Assert.assertEquals(expectedCode, code);
        Assert.assertEquals(expecredRule, rule);
    }

    private void assertOnField(MessageResult mr, int fieldNumber, LintRule expecredRule, String expectedCode) {
        List<RuleInfo> infoForField = getInfoForField(mr, fieldNumber);
        String code = null;
        LintRule rule = null;
        for (RuleInfo ruleInfo : infoForField) {
            if (ruleInfo.getCode().equals(expectedCode)
                    && ruleInfo.getLintRule().equals(expecredRule)) {
                return;
            }
            code = ruleInfo.getCode();
            rule = ruleInfo.getLintRule();
        }
        Assert.assertEquals(expectedCode, code);
        Assert.assertEquals(expecredRule, rule);
    }

    private void assertOnMessage(MessageResult mr, LintRule expectedRule, String expectedCode) {
        String code = null;
        LintRule rule = null;
        for (RuleInfo ruleInfo : mr.getInfoList()) {
            if (ruleInfo.getCode().equals(expectedCode)
                    && ruleInfo.getLintRule().equals(expectedRule)) {
                return;
            }
            code = ruleInfo.getCode();
            rule = ruleInfo.getLintRule();
        }
        Assert.assertEquals(expectedCode, code);
        Assert.assertEquals(expectedRule, rule);
    }

    @Test
    public void packageScopeVersionValid() throws IOException {
        Descriptors.Descriptor descriptor = Lint.LintFieldNamesBad.getDescriptor();
        Report result =  lintPackage(descriptor);
        Assert.assertEquals(0, result.getFileResultsCount());
    }

    @Test
    public void packageScopeVersionInvalid() throws IOException {
        Descriptors.Descriptor descriptor = Invalid.InvalidMessage.getDescriptor();
        Report result =  lintPackage(descriptor);
        FileResult fr = result.getFileResultsOrThrow("anemos/metastore/invalid/invalid.proto");

        Assert.assertEquals(1, result.getFileResultsCount());
        assertOnFile(fr, LintRule.LINT_PACKAGE_NAME_SHOULD_BE_VALID, "L70001/00");
    }

    private void assertOnFile(FileResult fr, LintRule expectedRule, String expectedCode){
        String code = null;
        LintRule rule = null;
        for (RuleInfo ruleInfo : fr.getInfoList()) {
            if (ruleInfo.getCode().equals(expectedCode)
                    && ruleInfo.getLintRule().equals(expectedRule)) {
                return;
            }
            code = ruleInfo.getCode();
            rule = ruleInfo.getLintRule();
        }
        Assert.assertEquals(expectedCode, code);
        Assert.assertEquals(expectedRule, rule);
    }

    private Report lintPackage(Descriptors.Descriptor ref) throws IOException {
        ProtoDescriptor pd = new ProtoDescriptor(ref);
        ValidationResults results = new ValidationResults();

        ProtoLint lint = new ProtoLint(pd, results);
        lint.lintOnPackage(ref);
        return results.getReport();
    }


}
