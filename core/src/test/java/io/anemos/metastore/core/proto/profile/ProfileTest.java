package io.anemos.metastore.core.proto.profile;

import static io.anemos.metastore.core.proto.validation.ProtoDiffTest.FILE_V1;
import static io.anemos.metastore.core.proto.validation.ProtoDiffTest.FILE_V1ALPHA;
import static org.junit.Assert.assertEquals;

import com.google.protobuf.DescriptorProtos;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.Report;
import java.io.IOException;
import org.junit.Test;

public class ProfileTest {

  @Test
  public void profileAllowAddRemoveFieldV1() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1
            .toBuilder()
            .setMessageType(0, FILE_V1.getMessageType(0).toBuilder().removeField(0))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    report = new ProfileAllowAdd().validate(report);
    assertEquals(1, report.getMessageResultsCount());
  }

  @Test
  public void profileAllowAddRemoveFieldV1Alpha() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1ALPHA).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1ALPHA
            .toBuilder()
            .setMessageType(0, FILE_V1ALPHA.getMessageType(0).toBuilder().removeField(0))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    report = new ProfileAllowAdd().validate(report);
    assertEquals(1, report.getMessageResultsCount());
  }

  @Test
  public void profileAllowStableAddAddAlphaRemoveFieldV1() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1
            .toBuilder()
            .setMessageType(0, FILE_V1.getMessageType(0).toBuilder().removeField(0))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    report = new ProfileAllowStableAddAlphaAll().validate(report);
    assertEquals(0, report.getMessageResultsCount());
  }

  @Test
  public void profileAllowStableAddAddAlphaRemoveFieldV1Alpha() throws Exception {
    ProtoDomain dRef = ProtoDomain.builder().add(FILE_V1ALPHA).build();
    DescriptorProtos.FileDescriptorProto fd =
        FILE_V1ALPHA
            .toBuilder()
            .setMessageType(0, FILE_V1ALPHA.getMessageType(0).toBuilder().removeField(0))
            .build();

    ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
    Report report = diff(dRef, dNew);
    report = new ProfileAllowStableAddAlphaAll().validate(report);
    assertEquals(1, report.getMessageResultsCount());
  }

  private Report diff(ProtoDomain dRef, ProtoDomain dNew) throws IOException {
    ValidationResults results = new ValidationResults();
    ProtoDiff diff = new ProtoDiff(dRef, dNew, results);
    diff.diffOnPackagePrefix("package.v1");
    diff.diffOnPackagePrefix("package.v1alpha");

    return results.createProto();
  }
}
