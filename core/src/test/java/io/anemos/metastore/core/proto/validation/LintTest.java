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
    assertOnField(mr, 1, "LINT_FIELD_NAME_SHOULD_BE_SNAKE");
    assertOnField(mr, 2, "LINT_FIELD_NAME_SHOULD_BE_SNAKE");
    assertOnField(mr, 3, "LINT_FIELD_NAME_SHOULD_BE_SNAKE");
  }

  @Test
  public void messageLowerCase() throws IOException {
    Patch result = lintMessage(Lint.lintmessagelowercase.getDescriptor());
    Assert.assertEquals(1, result.getMessagePatchesCount());

    MessagePatch mr = result.getMessagePatchesOrThrow("anemos.metastore.core.lintmessagelowercase");
    assertOnMessage(mr, "LINT_MESSAGE_NAME_SHOULD_BE_PASCAL");
  }

  @Test
  public void messageCamelCase() throws IOException {
    Patch result = lintMessage(Lint.lint_message_camelcase.getDescriptor());
    Assert.assertEquals(1, result.getMessagePatchesCount());

    MessagePatch mr =
        result.getMessagePatchesOrThrow("anemos.metastore.core.lint_message_camelcase");
    assertOnMessage(mr, "LINT_MESSAGE_NAME_SHOULD_BE_PASCAL");
  }

  @Test
  public void methodInputAndReturnTypeNoRR() throws IOException {
    Patch result =
        lintService(Lint.LintFieldNamesGood.getDescriptor(), "anemos.metastore.core.MethodService");

    ServicePatch mr = result.getServicePatchesOrThrow("anemos.metastore.core.MethodService");
    Assert.assertEquals(2, getInfoForMethod(mr, "MethodEmpty").size());
    assertOnMethod(mr, "MethodEmpty", "LINT_METHOD_RTYPE_END_WITH_RESPONSE");
    assertOnMethod(mr, "MethodEmpty", "LINT_METHOD_ITYPE_END_WITH_REQUEST");
  }

  @Test
  public void methodInputTypeNoR() throws IOException {
    Patch result =
        lintService(Lint.LintFieldNamesGood.getDescriptor(), "anemos.metastore.core.MethodService");

    ServicePatch mr = result.getServicePatchesOrThrow("anemos.metastore.core.MethodService");
    Assert.assertEquals(1, getInfoForMethod(mr, "MethodEmptyI").size());
    assertOnMethod(mr, "MethodEmptyI", "LINT_METHOD_ITYPE_END_WITH_REQUEST");
  }

  @Test
  public void methodReturnTypeNoR() throws IOException {
    Patch result =
        lintService(Lint.LintFieldNamesGood.getDescriptor(), "anemos.metastore.core.MethodService");

    ServicePatch mr = result.getServicePatchesOrThrow("anemos.metastore.core.MethodService");
    Assert.assertEquals(1, getInfoForMethod(mr, "MethodEmptyR").size());
    assertOnMethod(mr, "MethodEmptyR", "LINT_METHOD_RTYPE_END_WITH_RESPONSE");
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
    return results.createProto();
  }

  private Patch lintService(Descriptors.Descriptor ref, String name) throws IOException {
    ProtoDomain pd = ProtoDomain.buildFrom(ref);

    ValidationResults results = new ValidationResults();
    ProtoLint lint = new ProtoLint(pd, results);
    lint.lintOnService(name);

    return results.createProto();
  }

  private List<Rule> getInfoForField(MessagePatch mr, int fieldNumber) {
    for (FieldPatch fieldResult : mr.getFieldPatchesList()) {
      if (fieldResult.getNumber() == fieldNumber) {
        return fieldResult.getRuleList();
      }
    }
    Assert.assertNotNull(null);
    return null;
  }

  private List<Rule> getInfoForMethod(ServicePatch mr, String methodName) {
    for (MethodPatch methodResult : mr.getMethodPatchesList()) {
      if (methodResult.getName().equals(methodName)) {
        return methodResult.getRuleList();
      }
    }
    return null;
  }

  private void assertOnMethod(ServicePatch mr, String methodName, String expecredRule) {
    List<Rule> infoForField = getInfoForMethod(mr, methodName);
    String code = null;
    String rule = null;
    for (Rule ruleInfo : infoForField) {
      if (ruleInfo.getRuleName().equals(expecredRule)) {
        return;
      }
      rule = ruleInfo.getRuleName();
    }
    Assert.assertEquals(expecredRule, rule);
  }

  private void assertOnField(MessagePatch mr, int fieldNumber, String expecredRule) {
    List<Rule> infoForField = getInfoForField(mr, fieldNumber);
    String code = null;
    String rule = null;
    for (Rule ruleInfo : infoForField) {
      if (ruleInfo.getRuleName().equals(expecredRule)) {
        return;
      }
      rule = ruleInfo.getRuleName();
    }
    Assert.assertEquals(expecredRule, rule);
  }

  private void assertOnMessage(MessagePatch mr, String expectedRule) {
    String code = null;
    String rule = null;
    for (Rule ruleInfo : mr.getRuleList()) {
      if (ruleInfo.getRuleName().equals(expectedRule)) {
        return;
      }
      rule = ruleInfo.getRuleName();
    }
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
    assertOnFile(fr, "LINT_PACKAGE_NO_DIR_ALIGNMENT");
  }

  private void assertOnFile(FilePatch fr, String expectedRule) {
    String code = null;
    String rule = null;
    for (Rule ruleInfo : fr.getRuleList()) {
      if (ruleInfo.getRuleName().equals(expectedRule)) {
        return;
      }
      rule = ruleInfo.getRuleName();
    }
    Assert.assertEquals(expectedRule, rule);
  }

  private Patch lintPackage(Descriptors.Descriptor ref) throws IOException {
    ProtoDomain pd = ProtoDomain.buildFrom(ref);
    ValidationResults results = new ValidationResults();

    ProtoLint lint = new ProtoLint(pd, results);
    lint.lintOnPackage(ref.getFile());
    return results.createProto();
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
    assertOnFile(fr, "LINT_PACKAGE_NO_VERSION_ALIGNMENT");
  }

  private Patch lintVersion(Descriptors.Descriptor ref) throws IOException {
    ProtoDomain pd = ProtoDomain.buildFrom(ref);
    ValidationResults results = new ValidationResults();

    ProtoLint lint = new ProtoLint(pd, results);
    lint.lintOnVersion(ref);
    return results.createProto();
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
    assertOnFile(fr, "LINT_IMPORT_NO_ALIGNMENT");
  }

  private Patch lintImport(Descriptors.Descriptor ref) throws IOException {
    ProtoDomain pd = ProtoDomain.buildFrom(ref);
    ValidationResults results = new ValidationResults();

    ProtoLint lint = new ProtoLint(pd, results);
    lint.lintOnImport(ref);
    return results.createProto();
  }
}
