package io.anemos.metastore.core.proto.validation;

import com.google.protobuf.Descriptors;
import io.anemos.metastore.core.Lint;
import io.anemos.metastore.core.proto.validate.ProtoLint;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.core.test.v1.VersionScopeInvalid;
import io.anemos.metastore.core.test.v1.VersionScopeValid;
import io.anemos.metastore.invalid.Invalid;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.unused.UsedValidImport;
import io.anemos.metastore.unused.invalid.UnusedInvalidImport;
import io.anemos.metastore.v1alpha1.*;
import java.io.IOException;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class LintTest {

  @Test
  public void fieldOkSnakeCase() throws IOException {
    Patch result = lintMessage(Lint.LintFieldNamesGood.getDescriptor());
    Assert.assertEquals(0, result.getMessagePatchesCount());
  }

  @Test
  public void fieldNokCamel() throws IOException {
    Patch result = lintMessage(Lint.LintFieldNamesBad.getDescriptor());
    Assert.assertEquals(1, result.getMessagePatchesCount());

    MessagePatch mr = result.getMessagePatchesOrThrow("anemos.metastore.core.LintFieldNamesBad");
    assertOnField(mr, 1, LintRule.LINT_FIELD_NAME_SHOULD_BE_SNAKE, "L20001/02");
    assertOnField(mr, 2, LintRule.LINT_FIELD_NAME_SHOULD_BE_SNAKE, "L20001/01");
    assertOnField(mr, 3, LintRule.LINT_FIELD_NAME_SHOULD_BE_SNAKE, "L20001/05");
  }

  @Test
  public void messageLowerCase() throws IOException {
    Patch result = lintMessage(Lint.lintmessagelowercase.getDescriptor());
    Assert.assertEquals(1, result.getMessagePatchesCount());

    MessagePatch mr = result.getMessagePatchesOrThrow("anemos.metastore.core.lintmessagelowercase");
    assertOnMessage(mr, LintRule.LINT_MESSAGE_NAME_SHOULD_BE_PASCAL, "L10001/00");
  }

  @Test
  public void messageCamelCase() throws IOException {
    Patch result = lintMessage(Lint.lint_message_camelcase.getDescriptor());
    Assert.assertEquals(1, result.getMessagePatchesCount());

    MessagePatch mr =
        result.getMessagePatchesOrThrow("anemos.metastore.core.lint_message_camelcase");
    assertOnMessage(mr, LintRule.LINT_MESSAGE_NAME_SHOULD_BE_PASCAL, "L10001/00");
  }

  @Test
  public void methodInputAndReturnTypeNoRR() throws IOException {
    Patch result =
        lintService(Lint.LintFieldNamesGood.getDescriptor(), "anemos.metastore.core.MethodService");

    ServicePatch mr = result.getServicePatchesOrThrow("anemos.metastore.core.MethodService");
    Assert.assertEquals(2, getInfoForMethod(mr, "MethodEmpty").size());
    assertOnMethod(mr, "MethodEmpty", LintRule.LINT_METHOD_RTYPE_END_WITH_RESPONSE, "L50003/00");
    assertOnMethod(mr, "MethodEmpty", LintRule.LINT_METHOD_ITYPE_END_WITH_REQUEST, "L50002/00");
  }

  @Test
  public void methodInputTypeNoR() throws IOException {
    Patch result =
        lintService(Lint.LintFieldNamesGood.getDescriptor(), "anemos.metastore.core.MethodService");

    ServicePatch mr = result.getServicePatchesOrThrow("anemos.metastore.core.MethodService");
    Assert.assertEquals(1, getInfoForMethod(mr, "MethodEmptyI").size());
    assertOnMethod(mr, "MethodEmptyI", LintRule.LINT_METHOD_ITYPE_END_WITH_REQUEST, "L50002/00");
  }

  @Test
  public void methodReturnTypeNoR() throws IOException {
    Patch result =
        lintService(Lint.LintFieldNamesGood.getDescriptor(), "anemos.metastore.core.MethodService");

    ServicePatch mr = result.getServicePatchesOrThrow("anemos.metastore.core.MethodService");
    Assert.assertEquals(1, getInfoForMethod(mr, "MethodEmptyR").size());
    assertOnMethod(mr, "MethodEmptyR", LintRule.LINT_METHOD_RTYPE_END_WITH_RESPONSE, "L50003/00");
  }

  @Test
  public void methodOk() throws IOException {
    Patch result =
        lintService(Lint.LintFieldNamesGood.getDescriptor(), "anemos.metastore.core.MethodService");

    ServicePatch mr = result.getServicePatchesOrThrow("anemos.metastore.core.MethodService");
    Assert.assertNull(getInfoForMethod(mr, "MethodOk"));
  }

  private Patch lintMessage(Descriptors.Descriptor d) throws IOException {
    ProtoDomain pd = ProtoDomain.buildFrom(d);
    String message = d.getFullName();

    ValidationResults results = new ValidationResults();
    ProtoLint lint = new ProtoLint(pd, results);
    lint.lintOnMessage(message);
    return results.getPatch();
  }

  private Patch lintService(Descriptors.Descriptor ref, String name) throws IOException {
    ProtoDomain pd = ProtoDomain.buildFrom(ref);

    ValidationResults results = new ValidationResults();
    ProtoLint lint = new ProtoLint(pd, results);
    lint.lintOnService(name);

    return results.getPatch();
  }

  private List<LintRuleInfo> getInfoForField(MessagePatch mr, int fieldNumber) {
    for (FieldResult fieldResult : mr.getFieldResultsList()) {
      if (fieldResult.getNumber() == fieldNumber) {
        return fieldResult.getInfoList();
      }
    }
    Assert.assertNotNull(null);
    return null;
  }

  private List<LintRuleInfo> getInfoForMethod(ServicePatch mr, String methodName) {
    for (MethodResult methodResult : mr.getMethodResultsList()) {
      if (methodResult.getName().equals(methodName)) {
        return methodResult.getInfoList();
      }
    }
    return null;
  }

  private void assertOnMethod(
      ServicePatch mr, String methodName, LintRule expecredRule, String expectedCode) {
    List<LintRuleInfo> infoForField = getInfoForMethod(mr, methodName);
    String code = null;
    LintRule rule = null;
    for (LintRuleInfo ruleInfo : infoForField) {
      if (ruleInfo.getCode().equals(expectedCode) && ruleInfo.getLintRule().equals(expecredRule)) {
        return;
      }
      code = ruleInfo.getCode();
      rule = ruleInfo.getLintRule();
    }
    Assert.assertEquals(expectedCode, code);
    Assert.assertEquals(expecredRule, rule);
  }

  private void assertOnField(
      MessagePatch mr, int fieldNumber, LintRule expecredRule, String expectedCode) {
    List<LintRuleInfo> infoForField = getInfoForField(mr, fieldNumber);
    String code = null;
    LintRule rule = null;
    for (LintRuleInfo ruleInfo : infoForField) {
      if (ruleInfo.getCode().equals(expectedCode) && ruleInfo.getLintRule().equals(expecredRule)) {
        return;
      }
      code = ruleInfo.getCode();
      rule = ruleInfo.getLintRule();
    }
    Assert.assertEquals(expectedCode, code);
    Assert.assertEquals(expecredRule, rule);
  }

  private void assertOnMessage(MessagePatch mr, LintRule expectedRule, String expectedCode) {
    String code = null;
    LintRule rule = null;
    for (LintRuleInfo ruleInfo : mr.getInfoList()) {
      if (ruleInfo.getCode().equals(expectedCode) && ruleInfo.getLintRule().equals(expectedRule)) {
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
    Patch result = lintPackage(descriptor);
    Assert.assertEquals(0, result.getFilePatchesCount());
  }

  @Test
  public void packageScopeVersionInvalid() throws IOException {
    Descriptors.Descriptor descriptor = Invalid.InvalidMessage.getDescriptor();
    Patch result = lintPackage(descriptor);
    FilePatch fr = result.getFilePatchesOrThrow("anemos/metastore/invalid/invalid.proto");

    Assert.assertEquals(1, result.getFilePatchesCount());
    assertOnFile(fr, LintRule.LINT_PACKAGE_NO_DIR_ALIGNMENT, "L70001/00");
  }

  private void assertOnFile(FilePatch fr, LintRule expectedRule, String expectedCode) {
    String code = null;
    LintRule rule = null;
    for (LintRuleInfo ruleInfo : fr.getInfoList()) {
      if (ruleInfo.getCode().equals(expectedCode) && ruleInfo.getLintRule().equals(expectedRule)) {
        return;
      }
      code = ruleInfo.getCode();
      rule = ruleInfo.getLintRule();
    }
    Assert.assertEquals(expectedCode, code);
    Assert.assertEquals(expectedRule, rule);
  }

  private Patch lintPackage(Descriptors.Descriptor ref) throws IOException {
    ProtoDomain pd = ProtoDomain.buildFrom(ref);
    ValidationResults results = new ValidationResults();

    ProtoLint lint = new ProtoLint(pd, results);
    lint.lintOnPackage(ref.getFile());
    return results.getPatch();
  }

  @Test
  public void versionScopeValid() throws IOException {
    Descriptors.Descriptor descriptor = VersionScopeValid.VersionValid.getDescriptor();
    Patch result = lintVersion(descriptor);
    Assert.assertEquals(0, result.getFilePatchesCount());
  }

  @Test
  public void versionScopeInvalid() throws IOException {
    Descriptors.Descriptor descriptor = VersionScopeInvalid.VersionInValid.getDescriptor();
    Patch result = lintVersion(descriptor);
    FilePatch fr =
        result.getFilePatchesOrThrow("anemos/metastore/core/test/v1/version_scope_invalid.proto");

    Assert.assertEquals(1, result.getFilePatchesCount());
    assertOnFile(fr, LintRule.LINT_PACKAGE_NO_VERSION_ALIGNMENT, "L70002/00");
  }

  private Patch lintVersion(Descriptors.Descriptor ref) throws IOException {
    ProtoDomain pd = ProtoDomain.buildFrom(ref);
    ValidationResults results = new ValidationResults();

    ProtoLint lint = new ProtoLint(pd, results);
    lint.lintOnVersion(ref);
    return results.getPatch();
  }

  @Test
  public void unusedImportValid() throws IOException {
    Descriptors.Descriptor descriptor = UsedValidImport.Valid.getDescriptor();
    Patch result = lintImport(descriptor);
    Assert.assertEquals(0, result.getFilePatchesCount());
  }

  @Test
  public void unusedImportInvalid() throws IOException {
    Descriptors.Descriptor descriptor = UnusedInvalidImport.Invalid.getDescriptor();
    Patch result = lintImport(descriptor);
    FilePatch fr =
        result.getFilePatchesOrThrow("anemos/metastore/unused/invalid/unused_invalid_import.proto");
    Assert.assertEquals(1, result.getFilePatchesCount());
    assertOnFile(fr, LintRule.LINT_IMPORT_NO_ALIGNMENT, "L80001/00");
  }

  private Patch lintImport(Descriptors.Descriptor ref) throws IOException {
    ProtoDomain pd = ProtoDomain.buildFrom(ref);
    ValidationResults results = new ValidationResults();

    ProtoLint lint = new ProtoLint(pd, results);
    lint.lintOnImport(ref);
    return results.getPatch();
  }
}
