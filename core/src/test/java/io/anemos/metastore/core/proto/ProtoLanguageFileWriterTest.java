package io.anemos.metastore.core.proto;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;

@RunWith(JUnit4.class)
public class ProtoLanguageFileWriterTest {

    @Test
    public void noPackageSetTest() throws Exception {
            DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder =
                    DescriptorProtos.FileDescriptorProto.newBuilder()
                            .setName("test")
                            .setSyntax("proto3");

            DescriptorProtos.DescriptorProto.Builder descriptor = DescriptorProtos.DescriptorProto.newBuilder();
            descriptor.setName("TestMessage");
            fileDescriptorProtoBuilder.addMessageType(descriptor);

        Descriptors.FileDescriptor[] dependencies = new Descriptors.FileDescriptor[0];
        Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(fileDescriptorProtoBuilder.build(), dependencies);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ProtoLanguageFileWriter.write(fileDescriptor, outputStream);
        Assert.assertFalse(outputStream.toString().contains("package"));
    }

}
