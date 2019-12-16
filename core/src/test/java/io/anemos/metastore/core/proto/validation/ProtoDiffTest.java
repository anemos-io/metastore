package io.anemos.metastore.core.proto.validation;

import com.google.protobuf.DescriptorProtos;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.ChangeType;
import io.anemos.metastore.v1alpha1.Report;
import io.anemos.metastore.v1alpha1.ServiceResult;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ProtoDiffTest {
    static final DescriptorProtos.FileDescriptorProto FILE =
            DescriptorProtos.FileDescriptorProto.newBuilder()
                    .setName("package/file1.proto")
                    .setPackage("package")
                    .addMessageType(DescriptorProtos.DescriptorProto.newBuilder()
                            .setName("Message1")
                            .build())
                    .addService(DescriptorProtos.ServiceDescriptorProto.newBuilder()
                            .setName("Service1")
                            .build())
                    .addEnumType(DescriptorProtos.EnumDescriptorProto.newBuilder()
                            .setName("Enum1")
                            .addValue(DescriptorProtos.EnumValueDescriptorProto.newBuilder()
                                    .setNumber(0)
                                    .setName("ENUM_VALUE_UNSET")
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
    public void addService() throws Exception {
        ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
        DescriptorProtos.FileDescriptorProto fd = FILE.toBuilder().addService(DescriptorProtos.ServiceDescriptorProto.newBuilder()
                .setName("Service2").build()).build();

        ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
        Report report = diff(dRef, dNew);
        ServiceResult serviceResult = report.getServiceResultsMap().get("package.Service2");
        Assert.assertEquals("package.Service2",serviceResult.getChange().getToName());
        Assert.assertEquals(ChangeType.ADDITION,serviceResult.getChange().getChangeType());
    }

    @Test
    public void removeService() throws Exception {
        ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
        DescriptorProtos.FileDescriptorProto fd = FILE.toBuilder().clearService().build();

        ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
        Report report = diff(dRef, dNew);
        ServiceResult serviceResult = report.getServiceResultsMap().get("package.Service1");
        Assert.assertEquals("package.Service1",serviceResult.getChange().getFromName());
        Assert.assertEquals(ChangeType.REMOVAL,serviceResult.getChange().getChangeType());
    }

    @Test
    public void addEnum() throws Exception {
        ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
        DescriptorProtos.FileDescriptorProto fd = FILE.toBuilder().addEnumType(DescriptorProtos.EnumDescriptorProto.newBuilder()
                .setName("Enum2")
                .addValue(DescriptorProtos.EnumValueDescriptorProto.newBuilder()
                        .setName("ENUM_VALUE2_UNSET")
                        .setNumber(0).build()).build()).build();

        ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
        Report report = diff(dRef, dNew);
        ServiceResult serviceResult = report.getServiceResultsMap().get("package.Service2");
        Assert.assertEquals("package.Service2",serviceResult.getChange().getToName());
        Assert.assertEquals(ChangeType.ADDITION,serviceResult.getChange().getChangeType());
    }

    @Test
    public void removeEnum() throws Exception {
        ProtoDomain dRef = ProtoDomain.builder().add(FILE).build();
        DescriptorProtos.FileDescriptorProto fd = FILE.toBuilder().clearService().build();

        ProtoDomain dNew = ProtoDomain.builder().add(fd).build();
        Report report = diff(dRef, dNew);
        ServiceResult serviceResult = report.getServiceResultsMap().get("package.Service1");
        Assert.assertEquals("package.Service1",serviceResult.getChange().getFromName());
        Assert.assertEquals(ChangeType.REMOVAL,serviceResult.getChange().getChangeType());
    }

    private Report diff(ProtoDomain dRef, ProtoDomain dNew) throws IOException {
        ValidationResults results = new ValidationResults();
        ProtoDiff diff = new ProtoDiff(dRef, dNew, results);
        diff.diffOnFileName("package/file1.proto");

        return results.getReport();
//        System.out.println(result);
//        return result.getMessageResultsMap().get("test.v1.ProtoBeamBasicMessage").getFieldResults(0);
    }


}
