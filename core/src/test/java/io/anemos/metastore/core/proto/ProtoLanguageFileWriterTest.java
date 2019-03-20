package io.anemos.metastore.core.proto;

import com.google.protobuf.*;
import io.anemos.Options;
import io.anemos.metastore.core.test.OptionsTest;
import io.anemos.metastore.core.test.TestOption;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

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

    @Test
    public void messageOptionTest() throws Exception {
        DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder =
                DescriptorProtos.FileDescriptorProto.newBuilder()
                        .setName("test")
                        .setSyntax("proto3");

        DescriptorProtos.DescriptorProto.Builder descriptor = DescriptorProtos.DescriptorProto.newBuilder();
        descriptor.setName("TestMessage");

        TestOption testOption = TestOption.newBuilder()
                .setString("testString")
                .addRepeatedString("test1")
                .addRepeatedString("test2")
                .setInt32(2)
                .addRepeatedInt32(3)
                .addRepeatedInt32(4)
                .setInt64(10)
                .build();
        DescriptorProtos.MessageOptions messageOptions = DescriptorProtos.MessageOptions.newBuilder()
                .setExtension(OptionsTest.testOption, testOption)
                .build();

        descriptor.setOptions(messageOptions);
        fileDescriptorProtoBuilder.addMessageType(descriptor);

        Descriptors.FileDescriptor[] dependencies = new Descriptors.FileDescriptor[1];
        dependencies[0] = OptionsTest.getDescriptor();
        Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(fileDescriptorProtoBuilder.build(), dependencies);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ProtoLanguageFileWriter.write(fileDescriptor, outputStream);

        String expected = "syntax = \"proto3\";\n" +
                "\n" +
                "import \"options_test.proto\";\n" +
                "\n" +
                "\n" +
                "\n" +
                "message TestMessage {\n" +
                "\toption (anemos.metastore.core.test.test_option).string = \"testString\";\n" +
                "\toption (anemos.metastore.core.test.test_option).repeated_string = \"test1\";\n" +
                "\toption (anemos.metastore.core.test.test_option).repeated_string = \"test2\";\n" +
                "\toption (anemos.metastore.core.test.test_option).int32 = 2;\n" +
                "\toption (anemos.metastore.core.test.test_option).repeated_int32 = 3;\n" +
                "\toption (anemos.metastore.core.test.test_option).repeated_int32 = 4;\n" +
                "\toption (anemos.metastore.core.test.test_option).int64 = 10;\n" +
                "\n" +
                "}\n";
        Assert.assertEquals(expected, outputStream.toString());
    }

    @Test
    public void fieldOptionsTest() throws Exception {
        DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder =
                DescriptorProtos.FileDescriptorProto.newBuilder()
                        .setName("test")
                        .setSyntax("proto3");

        DescriptorProtos.DescriptorProto.Builder descriptor = DescriptorProtos.DescriptorProto.newBuilder();
        descriptor.setName("TestMessage");

        DescriptorProtos.FieldDescriptorProto.Builder fieldDescriptorProtoBuilder = DescriptorProtos.FieldDescriptorProto.newBuilder()
                .setName("string")
                .setNumber(123)
                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)
                .setOptions(DescriptorProtos.FieldOptions.newBuilder()
                        .setExtension(OptionsTest.fieldOption1, 123)
                        .setExtension(OptionsTest.fieldOption2, "something")
                        .build());

        descriptor.addField(fieldDescriptorProtoBuilder);
        fileDescriptorProtoBuilder.addMessageType(descriptor);

        ArrayList<Descriptors.FileDescriptor> dependencies = new ArrayList<>();
        dependencies.add(OptionsTest.getDescriptor());
        Descriptors.FileDescriptor[] list = new Descriptors.FileDescriptor[dependencies.size()];
        dependencies.toArray(list);
        Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(fileDescriptorProtoBuilder.build(), list);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ProtoLanguageFileWriter.write(fileDescriptor, outputStream);

        String expected = "syntax = \"proto3\";\n" +
                "\n" +
                "import \"options_test.proto\";\n" +
                "\n" +
                "\n" +
                "\n" +
                "message TestMessage {\n\n" +
                "\tstring string = 123 [(anemos.metastore.core.test.field_option_1) = 123, (anemos.metastore.core.test.field_option_2) = something];\n" +
                "}\n";
        Assert.assertEquals(expected, outputStream.toString());
    }

}
