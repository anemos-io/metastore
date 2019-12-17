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
  static final DescriptorProtos.FileDescriptorProto FILE =
      DescriptorProtos.FileDescriptorProto.newBuilder()
          .setName("package/file1.proto")
          .setPackage("package")
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
                          .setInputType("package.Request")
                          .setOutputType("package.Response")
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
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    ProtoDomain dNew = ProtoDomain.builder().add(FILE).build();
    Report report = diff(dRef, dNew);
    System.out.println(report);
  }

  @Test
  public void addEnum() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE.toBuilder()
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
    EnumResult result = report.getEnumResultsMap().get("package.Enum2");
    Assert.assertEquals("package.Enum2", result.getChange().getToName());
    Assert.assertEquals(ChangeType.ADDITION, result.getChange().getChangeType());
  }

  @Test
  public void removeEnum() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    DescriptorProtos.FileDescriptorProto fd = FILE.toBuilder().clearEnumType().build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    EnumResult result = report.getEnumResultsMap().get("package.Enum1");
    Assert.assertEquals("package.Enum1", result.getChange().getFromName());
    Assert.assertEquals(ChangeType.REMOVAL, result.getChange().getChangeType());
  }

  @Test
  public void addEnumValue() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE.toBuilder()
            .setEnumType(
                0,
                FILE.getEnumType(0)
                    .toBuilder()
                    .addValue(
                        DescriptorProtos.EnumValueDescriptorProto.newBuilder()
                            .setName("ENUM_VALUE1_VALUE2")
                            .setNumber(2)))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    EnumResult result = report.getEnumResultsMap().get("package.Enum1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(ChangeType.ADDITION, result.getValueResults(0).getChange().getChangeType());
    Assert.assertEquals(2, result.getValueResults(0).getNumber());
    Assert.assertEquals("ENUM_VALUE1_VALUE2", result.getValueResults(0).getName());
    Assert.assertEquals("", result.getValueResults(0).getChange().getFromName());
    Assert.assertEquals("ENUM_VALUE1_VALUE2", result.getValueResults(0).getChange().getToName());
  }

  @Test
  public void removeEnumValue() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE.toBuilder().setEnumType(0, FILE.getEnumType(0).toBuilder().removeValue(1)).build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    EnumResult result = report.getEnumResultsMap().get("package.Enum1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(ChangeType.REMOVAL, result.getValueResults(0).getChange().getChangeType());
    Assert.assertEquals(1, result.getValueResults(0).getNumber());
    Assert.assertEquals("ENUM_VALUE1_VALUE1", result.getValueResults(0).getName());
    Assert.assertEquals("ENUM_VALUE1_VALUE1", result.getValueResults(0).getChange().getFromName());
    Assert.assertEquals("", result.getValueResults(0).getChange().getToName());
  }

  @Test
  public void renameEnumValue() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE.toBuilder()
            .setEnumType(
                0,
                FILE.getEnumType(0)
                    .toBuilder()
                    .setValue(1, FILE.getEnumType(0).getValue(1).toBuilder().setName("FOO")))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    EnumResult result = report.getEnumResultsMap().get("package.Enum1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(ChangeType.CHANGED, result.getValueResults(0).getChange().getChangeType());
    Assert.assertEquals(1, result.getValueResults(0).getNumber());
    Assert.assertEquals("FOO", result.getValueResults(0).getName());
    Assert.assertEquals("ENUM_VALUE1_VALUE1", result.getValueResults(0).getChange().getFromName());
    Assert.assertEquals("FOO", result.getValueResults(0).getChange().getToName());
  }

  @Test
  public void addMessage() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE.toBuilder()
            .addMessageType(
                DescriptorProtos.DescriptorProto.newBuilder().setName("Message2").build())
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    MessageResult result = report.getMessageResultsMap().get("package.Message2");
    Assert.assertEquals("package.Message2", result.getChange().getToName());
    Assert.assertEquals(ChangeType.ADDITION, result.getChange().getChangeType());
  }

  @Test
  public void removeMessage() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    DescriptorProtos.FileDescriptorProto fd = FILE.toBuilder().removeMessageType(0).build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    MessageResult result = report.getMessageResultsMap().get("package.Message1");
    Assert.assertEquals("package.Message1", result.getChange().getFromName());
    Assert.assertEquals(ChangeType.REMOVAL, result.getChange().getChangeType());
  }

  @Test
  public void addField() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE.toBuilder()
            .setMessageType(
                0,
                FILE.getMessageType(0)
                    .toBuilder()
                    .addField(
                        DescriptorProtos.FieldDescriptorProto.newBuilder()
                            .setName("second_field")
                            .setNumber(2)
                            .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    MessageResult result = report.getMessageResultsMap().get("package.Message1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(ChangeType.ADDITION, result.getFieldResults(0).getChange().getChangeType());
    Assert.assertEquals(2, result.getFieldResults(0).getNumber());
    Assert.assertEquals("second_field", result.getFieldResults(0).getName());
    Assert.assertEquals("", result.getFieldResults(0).getChange().getFromName());
    Assert.assertEquals("second_field", result.getFieldResults(0).getChange().getToName());
  }

  @Test
  public void removeField() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE.toBuilder()
            .setMessageType(0, FILE.getMessageType(0).toBuilder().removeField(0))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    MessageResult result = report.getMessageResultsMap().get("package.Message1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(ChangeType.REMOVAL, result.getFieldResults(0).getChange().getChangeType());
    Assert.assertEquals(1, result.getFieldResults(0).getNumber());
    Assert.assertEquals("first_field", result.getFieldResults(0).getName());
    Assert.assertEquals("first_field", result.getFieldResults(0).getChange().getFromName());
    Assert.assertEquals("", result.getFieldResults(0).getChange().getToName());
  }

  @Test
  public void renameField() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE.toBuilder()
            .setMessageType(
                0,
                FILE.getMessageType(0)
                    .toBuilder()
                    .setField(
                        0, FILE.getMessageType(0).getField(0).toBuilder().setName("changed_field")))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    MessageResult result = report.getMessageResultsMap().get("package.Message1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(ChangeType.CHANGED, result.getFieldResults(0).getChange().getChangeType());
    Assert.assertEquals(1, result.getFieldResults(0).getNumber());
    Assert.assertEquals("changed_field", result.getFieldResults(0).getName());
    Assert.assertEquals("first_field", result.getFieldResults(0).getChange().getFromName());
    Assert.assertEquals("changed_field", result.getFieldResults(0).getChange().getToName());
  }

  @Test
  public void addService() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE.toBuilder()
            .addService(
                DescriptorProtos.ServiceDescriptorProto.newBuilder().setName("Service2").build())
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    ServiceResult serviceResult = report.getServiceResultsMap().get("package.Service2");
    Assert.assertEquals("package.Service2", serviceResult.getChange().getToName());
    Assert.assertEquals(ChangeType.ADDITION, serviceResult.getChange().getChangeType());
  }

  @Test
  public void removeService() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    DescriptorProtos.FileDescriptorProto fd = FILE.toBuilder().clearService().build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    ServiceResult serviceResult = report.getServiceResultsMap().get("package.Service1");
    Assert.assertEquals("package.Service1", serviceResult.getChange().getFromName());
    Assert.assertEquals(ChangeType.REMOVAL, serviceResult.getChange().getChangeType());
  }

  @Test
  public void addMethod() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE.toBuilder()
            .setService(
                0,
                FILE.getService(0)
                    .toBuilder()
                    .addMethod(
                        DescriptorProtos.MethodDescriptorProto.newBuilder()
                            .setName("Method2")
                            .setInputType("package.Request")
                            .setOutputType("package.Response")))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    ServiceResult result = report.getServiceResultsMap().get("package.Service1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(
        ChangeType.ADDITION, result.getMethodResults(0).getChange().getChangeType());
    Assert.assertEquals("Method2", result.getMethodResults(0).getName());
    Assert.assertEquals("", result.getMethodResults(0).getChange().getFromName());
    Assert.assertEquals("Method2", result.getMethodResults(0).getChange().getToName());
  }

  @Test
  public void removeMethod() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE.toBuilder().setService(0, FILE.getService(0).toBuilder().removeMethod(0)).build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    ServiceResult result = report.getServiceResultsMap().get("package.Service1");
    Assert.assertEquals(ChangeType.UNCHANGED, result.getChange().getChangeType());
    Assert.assertEquals(ChangeType.REMOVAL, result.getMethodResults(0).getChange().getChangeType());
    Assert.assertEquals("Method1", result.getMethodResults(0).getName());
    Assert.assertEquals("Method1", result.getMethodResults(0).getChange().getFromName());
    Assert.assertEquals("", result.getMethodResults(0).getChange().getToName());
  }

  @Test
  public void renameMethod() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE.toBuilder()
            .setService(
                0,
                FILE.getService(0)
                    .toBuilder()
                    .setMethod(0, FILE.getService(0).getMethod(0).toBuilder().setName("MethodX")))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    ServiceResult result = report.getServiceResultsMap().get("package.Service1");
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
    diff.diffOnFileName("package/file1.proto");
    return results.getReport();
  }
}
