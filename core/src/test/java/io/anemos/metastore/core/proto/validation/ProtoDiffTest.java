package io.anemos.metastore.core.proto.validation;

import com.google.protobuf.DescriptorProtos;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.ChangeType;
import io.anemos.metastore.v1alpha1.EnumResult;
import io.anemos.metastore.v1alpha1.MessageResult;
import io.anemos.metastore.v1alpha1.Report;
import io.anemos.metastore.v1alpha1.ServiceResult;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

public class ProtoDiffTest {
  public static final DescriptorProtos.FileDescriptorProto FILE_V1 =
      DescriptorProtos.FileDescriptorProto.newBuilder()
          .setName("package/v1/file1.proto")
          .setPackage("package.v1")
          .addMessageType(
              DescriptorProtos.DescriptorProto.newBuilder()
                  .setName("Message1")
                  .addField(
                      DescriptorProtos.FieldDescriptorProto.newBuilder()
                          .setNumber(1)
                          .setName("first_field")
                          .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32)
                          .build())
                  .build())
          .addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("Request"))
          .addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("Response"))
          .addService(
              DescriptorProtos.ServiceDescriptorProto.newBuilder()
                  .setName("Service1")
                  .addMethod(
                      DescriptorProtos.MethodDescriptorProto.newBuilder()
                          .setName("Method1")
                          .setInputType("package.v1.Request")
                          .setOutputType("package.v1.Response")
                          .build())
                  .build())
          .addEnumType(
              DescriptorProtos.EnumDescriptorProto.newBuilder()
                  .setName("Enum1")
                  .addValue(
                      DescriptorProtos.EnumValueDescriptorProto.newBuilder()
                          .setNumber(0)
                          .setName("ENUM_VALUE1_UNSET")
                          .build())
                  .addValue(
                      DescriptorProtos.EnumValueDescriptorProto.newBuilder()
                          .setNumber(1)
                          .setName("ENUM_VALUE1_VALUE1")
                          .build())
                  .build())
          .build();

  public static final DescriptorProtos.FileDescriptorProto FILE_V1ALPHA =
      DescriptorProtos.FileDescriptorProto.newBuilder()
          .setName("package/v1alpha/file1.proto")
          .setPackage("package.v1alpha")
          .addMessageType(
              DescriptorProtos.DescriptorProto.newBuilder()
                  .setName("Message1")
                  .addField(
                      DescriptorProtos.FieldDescriptorProto.newBuilder()
                          .setNumber(1)
                          .setName("first_field")
                          .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32)
                          .build())
                  .build())
          .addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("Request"))
          .addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("Response"))
          .addService(
              DescriptorProtos.ServiceDescriptorProto.newBuilder()
                  .setName("Service1")
                  .addMethod(
                      DescriptorProtos.MethodDescriptorProto.newBuilder()
                          .setName("Method1")
                          .setInputType("package.v1alpha.Request")
                          .setOutputType("package.v1alpha.Response")
                          .build())
                  .build())
          .addEnumType(
              DescriptorProtos.EnumDescriptorProto.newBuilder()
                  .setName("Enum1")
                  .addValue(
                      DescriptorProtos.EnumValueDescriptorProto.newBuilder()
                          .setNumber(0)
                          .setName("ENUM_VALUE1_UNSET")
                          .build())
                  .addValue(
                      DescriptorProtos.EnumValueDescriptorProto.newBuilder()
                          .setNumber(1)
                          .setName("ENUM_VALUE1_VALUE1")
                          .build())
                  .build())
          .build();

  @Test
  public void noDiff() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    ProtoDomain dNew = ProtoDomain.builder().add(FILE_V1).build();
    Report report = diff(dRef, dNew);
  }

  @Test
  public void addEnum() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1
            .toBuilder()
            .addEnumType(
                DescriptorProtos.EnumDescriptorProto.newBuilder()
                    .setName("Enum2")
                    .addValue(
                        DescriptorProtos.EnumValueDescriptorProto.newBuilder()
                            .setName("ENUM_VALUE2_UNSET")
                            .setNumber(0)
                            .build())
                    .build())
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    EnumResult result = report.getEnumResultsMap().get("package.v1.Enum2");
    Assert.assertEquals("package.v1.Enum2", result.getChange().getToName());
    Assert.assertEquals(ChangeType.ADDITION, result.getChange().getChangeType());
  }

  @Test
  public void removeEnum() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd = FILE_V1.toBuilder().clearEnumType().build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    EnumResult result = report.getEnumResultsMap().get("package.v1.Enum1");
    Assert.assertEquals("package.v1.Enum1", result.getChange().getFromName());
    Assert.assertEquals(ChangeType.REMOVAL, result.getChange().getChangeType());
  }

  @Test
  public void addEnumValue() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1
            .toBuilder()
            .setEnumType(
                0,
                FILE_V1
                    .getEnumType(0)
                    .toBuilder()
                    .addValue(
                        DescriptorProtos.EnumValueDescriptorProto.newBuilder()
                            .setName("ENUM_VALUE1_VALUE2")
                            .setNumber(2)))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    EnumResult result = report.getEnumResultsMap().get("package.v1.Enum1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(ChangeType.ADDITION, result.getValueResults(0).getChange().getChangeType());
    Assert.assertEquals(2, result.getValueResults(0).getNumber());
    Assert.assertEquals("ENUM_VALUE1_VALUE2", result.getValueResults(0).getName());
    Assert.assertEquals("", result.getValueResults(0).getChange().getFromName());
    Assert.assertEquals("ENUM_VALUE1_VALUE2", result.getValueResults(0).getChange().getToName());
  }

  @Test
  public void removeEnumValue() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1
            .toBuilder()
            .setEnumType(0, FILE_V1.getEnumType(0).toBuilder().removeValue(1))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    EnumResult result = report.getEnumResultsMap().get("package.v1.Enum1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(ChangeType.REMOVAL, result.getValueResults(0).getChange().getChangeType());
    Assert.assertEquals(1, result.getValueResults(0).getNumber());
    Assert.assertEquals("ENUM_VALUE1_VALUE1", result.getValueResults(0).getName());
    Assert.assertEquals("ENUM_VALUE1_VALUE1", result.getValueResults(0).getChange().getFromName());
    Assert.assertEquals("", result.getValueResults(0).getChange().getToName());
  }

  @Test
  public void renameEnumValue() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1
            .toBuilder()
            .setEnumType(
                0,
                FILE_V1
                    .getEnumType(0)
                    .toBuilder()
                    .setValue(1, FILE_V1.getEnumType(0).getValue(1).toBuilder().setName("FOO")))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    EnumResult result = report.getEnumResultsMap().get("package.v1.Enum1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(ChangeType.CHANGED, result.getValueResults(0).getChange().getChangeType());
    Assert.assertEquals(1, result.getValueResults(0).getNumber());
    Assert.assertEquals("FOO", result.getValueResults(0).getName());
    Assert.assertEquals("ENUM_VALUE1_VALUE1", result.getValueResults(0).getChange().getFromName());
    Assert.assertEquals("FOO", result.getValueResults(0).getChange().getToName());
  }

  @Test
  public void addMessage() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1
            .toBuilder()
            .addMessageType(
                DescriptorProtos.DescriptorProto.newBuilder().setName("Message2").build())
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    MessageResult result = report.getMessageResultsMap().get("package.v1.Message2");
    Assert.assertEquals("package.v1.Message2", result.getChange().getToName());
    Assert.assertEquals(ChangeType.ADDITION, result.getChange().getChangeType());
  }

  @Test
  public void removeMessage() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd = FILE_V1.toBuilder().removeMessageType(0).build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    MessageResult result = report.getMessageResultsMap().get("package.v1.Message1");
    Assert.assertEquals("package.v1.Message1", result.getChange().getFromName());
    Assert.assertEquals(ChangeType.REMOVAL, result.getChange().getChangeType());
  }

  @Test
  public void addField() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1
            .toBuilder()
            .setMessageType(
                0,
                FILE_V1
                    .getMessageType(0)
                    .toBuilder()
                    .addField(
                        DescriptorProtos.FieldDescriptorProto.newBuilder()
                            .setName("second_field")
                            .setNumber(2)
                            .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    MessageResult result = report.getMessageResultsMap().get("package.v1.Message1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(ChangeType.ADDITION, result.getFieldResults(0).getChange().getChangeType());
    Assert.assertEquals(2, result.getFieldResults(0).getNumber());
    Assert.assertEquals("second_field", result.getFieldResults(0).getName());
    Assert.assertEquals("", result.getFieldResults(0).getChange().getFromName());
    Assert.assertEquals("second_field", result.getFieldResults(0).getChange().getToName());
  }

  @Test
  public void removeField() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1
            .toBuilder()
            .setMessageType(0, FILE_V1.getMessageType(0).toBuilder().removeField(0))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    MessageResult result = report.getMessageResultsMap().get("package.v1.Message1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(ChangeType.REMOVAL, result.getFieldResults(0).getChange().getChangeType());
    Assert.assertEquals(1, result.getFieldResults(0).getNumber());
    Assert.assertEquals("first_field", result.getFieldResults(0).getName());
    Assert.assertEquals("first_field", result.getFieldResults(0).getChange().getFromName());
    Assert.assertEquals("", result.getFieldResults(0).getChange().getToName());
  }

  @Test
  public void renameField() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1
            .toBuilder()
            .setMessageType(
                0,
                FILE_V1
                    .getMessageType(0)
                    .toBuilder()
                    .setField(
                        0,
                        FILE_V1.getMessageType(0).getField(0).toBuilder().setName("changed_field")))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    MessageResult result = report.getMessageResultsMap().get("package.v1.Message1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(ChangeType.CHANGED, result.getFieldResults(0).getChange().getChangeType());
    Assert.assertEquals(1, result.getFieldResults(0).getNumber());
    Assert.assertEquals("changed_field", result.getFieldResults(0).getName());
    Assert.assertEquals("first_field", result.getFieldResults(0).getChange().getFromName());
    Assert.assertEquals("changed_field", result.getFieldResults(0).getChange().getToName());
  }

  @Test
  public void addService() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1
            .toBuilder()
            .addService(
                DescriptorProtos.ServiceDescriptorProto.newBuilder().setName("Service2").build())
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    ServiceResult serviceResult = report.getServiceResultsMap().get("package.v1.Service2");
    Assert.assertEquals("package.v1.Service2", serviceResult.getChange().getToName());
    Assert.assertEquals(ChangeType.ADDITION, serviceResult.getChange().getChangeType());
  }

  @Test
  public void removeService() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd = FILE_V1.toBuilder().clearService().build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    ServiceResult serviceResult = report.getServiceResultsMap().get("package.v1.Service1");
    Assert.assertEquals("package.v1.Service1", serviceResult.getChange().getFromName());
    Assert.assertEquals(ChangeType.REMOVAL, serviceResult.getChange().getChangeType());
  }

  @Test
  public void addMethod() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1
            .toBuilder()
            .setService(
                0,
                FILE_V1
                    .getService(0)
                    .toBuilder()
                    .addMethod(
                        DescriptorProtos.MethodDescriptorProto.newBuilder()
                            .setName("Method2")
                            .setInputType("package.v1.Request")
                            .setOutputType("package.v1.Response")))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    ServiceResult result = report.getServiceResultsMap().get("package.v1.Service1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(
        ChangeType.ADDITION, result.getMethodResults(0).getChange().getChangeType());
    Assert.assertEquals("Method2", result.getMethodResults(0).getName());
    Assert.assertEquals("", result.getMethodResults(0).getChange().getFromName());
    Assert.assertEquals("Method2", result.getMethodResults(0).getChange().getToName());
  }

  @Test
  public void removeMethod() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1
            .toBuilder()
            .setService(0, FILE_V1.getService(0).toBuilder().removeMethod(0))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    ServiceResult result = report.getServiceResultsMap().get("package.v1.Service1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(ChangeType.REMOVAL, result.getMethodResults(0).getChange().getChangeType());
    Assert.assertEquals("Method1", result.getMethodResults(0).getName());
    Assert.assertEquals("Method1", result.getMethodResults(0).getChange().getFromName());
    Assert.assertEquals("", result.getMethodResults(0).getChange().getToName());
  }

  @Test
  public void renameMethod() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1
            .toBuilder()
            .setService(
                0,
                FILE_V1
                    .getService(0)
                    .toBuilder()
                    .setMethod(
                        0, FILE_V1.getService(0).getMethod(0).toBuilder().setName("MethodX")))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    ServiceResult result = report.getServiceResultsMap().get("package.v1.Service1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());

    Assert.assertEquals(
        ChangeType.ADDITION, result.getMethodResults(0).getChange().getChangeType());
    Assert.assertEquals("MethodX", result.getMethodResults(0).getName());
    Assert.assertEquals("", result.getMethodResults(0).getChange().getFromName());
    Assert.assertEquals("MethodX", result.getMethodResults(0).getChange().getToName());

    Assert.assertEquals(ChangeType.REMOVAL, result.getMethodResults(1).getChange().getChangeType());
    Assert.assertEquals("Method1", result.getMethodResults(1).getName());
    Assert.assertEquals("Method1", result.getMethodResults(1).getChange().getFromName());
    Assert.assertEquals("", result.getMethodResults(1).getChange().getToName());
  }

  private Report diff(ProtoDomain dRef, ProtoDomain dNew) throws IOException {
    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(dRef, dNew, results);
    diff.diffOnFileName("package/v1/file1.proto");
    return results.createProto();
  }
}
