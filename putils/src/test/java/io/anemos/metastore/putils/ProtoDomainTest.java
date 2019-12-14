package io.anemos.metastore.putils;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ProtoDomainTest {

  @Test
  public void testEmpty() throws Exception {
    ProtoDomain domain = ProtoDomain.empty();
    Assert.assertEquals(0, domain.getFileNames().size());
  }

  @Test
  public void testFromList() throws Exception {
    ProtoDomain domain = ProtoDomain.buildFrom(TestProto.FILE_DESCRIPTOR_LIST);
    Assert.assertEquals(2, domain.getFileNames().size());
  }

  @Test
  public void testUpdateWithAdd() throws Exception {
    ProtoDomain domain = ProtoDomain.buildFrom(TestProto.FILE_DESCRIPTOR_LIST);

    Collection<ByteString> update = new ArrayList<>();
    update.add(TestProto.FILE_DESCRIPTOR_PROTO3.toByteString());
    domain = domain.toBuilder().mergeBinary(update).build();
    Assert.assertEquals(3, domain.getFileNames().size());
  }

  @Test
  public void testUpdateWithMerge() throws Exception {
    ProtoDomain domain = ProtoDomain.buildFrom(TestProto.FILE_DESCRIPTOR_LIST);

    Collection<ByteString> update = new ArrayList<>();
    update.add(TestProto.FILE_DESCRIPTOR_PROTO2.toByteString());
    update.add(TestProto.FILE_DESCRIPTOR_PROTO3.toByteString());
    domain = domain.toBuilder().mergeBinary(update).build();
    Assert.assertEquals(3, domain.getFileNames().size());
  }

  @Test
  public void getFileOptionByName() throws IOException {
    ProtoDomain domain = TestSets.baseAddFileOption();
    Descriptors.FieldDescriptor fd =
        domain.getOptions().getFileOptionByName("test.v1.file_option_1");
    Assert.assertEquals("test.v1.file_option_1", fd.getFullName());
  }

  @Test
  public void getMessageOptionByName() throws IOException {
    ProtoDomain domain = TestSets.baseAddFileOption();
    Descriptors.FieldDescriptor fd =
        domain.getOptions().getMessageOptionByName("test.v1.message_option_1");
    Assert.assertEquals("test.v1.message_option_1", fd.getFullName());
  }

  @Test
  public void getFieldOptionByName() throws IOException {
    ProtoDomain domain = TestSets.baseAddFileOption();
    Descriptors.FieldDescriptor fd =
        domain.getOptions().getFieldOptionByName("test.v1.field_option_1");
    Assert.assertEquals("test.v1.field_option_1", fd.getFullName());
  }

  @Test
  public void getEnumOptionByName() throws IOException {
    ProtoDomain domain = TestSets.baseAddFileOption();
    Descriptors.FieldDescriptor fd =
        domain.getOptions().getEnumOptionByName("test.v1.enum_option_1");
    Assert.assertEquals("test.v1.enum_option_1", fd.getFullName());
  }

  @Test
  public void getEnumValueOptionByName() throws IOException {
    ProtoDomain domain = TestSets.baseAddFileOption();
    Descriptors.FieldDescriptor fd =
        domain.getOptions().getEnumValueOptionByName("test.v1.enum_value_option_1");
    Assert.assertEquals("test.v1.enum_value_option_1", fd.getFullName());
  }

  @Test
  public void getServiceOptionByName() throws IOException {
    ProtoDomain domain = TestSets.baseAddFileOption();
    Descriptors.FieldDescriptor fd =
        domain.getOptions().getServiceOptionByName("test.v1.service_option_1");
    Assert.assertEquals("test.v1.service_option_1", fd.getFullName());
  }

  @Test
  public void getMethodOptionByName() throws IOException {
    ProtoDomain domain = TestSets.baseAddFileOption();
    Descriptors.FieldDescriptor fd =
        domain.getOptions().getMethodOptionByName("test.v1.method_option_1");
    Assert.assertEquals("test.v1.method_option_1", fd.getFullName());
  }

  @Test
  public void findFileDescriptorsByOption() throws IOException {
    ProtoDomain domain = TestSets.baseMultipleOptions();
    Collection<Descriptors.FileDescriptor> options =
        domain.findFileDescriptorsByOption("test.v1.file_option");
    Assert.assertEquals(1, options.size());
  }

  @Test
  public void findDescriptorsByOption() throws IOException {
    ProtoDomain domain = TestSets.baseMultipleOptions();
    Collection<Descriptors.Descriptor> options =
        domain.findDescriptorsByOption("test.v1.message_option");
    Assert.assertEquals(1, options.size());
  }

  @Test
  public void findEnumDescriptorsByOption() throws IOException {
    ProtoDomain domain = TestSets.baseMultipleOptions();
    Collection<Descriptors.EnumDescriptor> options =
        domain.findEnumDescriptorsByOption("test.v1.enum_option");
    // TODO Add more sets with enums
    Assert.assertEquals(0, options.size());
  }

  @Test
  public void findServiceDescriptorsByOption() throws IOException {
    ProtoDomain domain = TestSets.baseMultipleOptions();
    Collection<Descriptors.ServiceDescriptor> options =
        domain.findServiceDescriptorsByOption("test.v1.service_option");
    // TODO Add more sets with services
    Assert.assertEquals(0, options.size());
  }

  @Test
  public void useOptionOnDescriptor() throws IOException {
    ProtoDomain domain = TestSets.baseMultipleOptions();
    Descriptors.FieldDescriptor fd =
        domain.getOptions().getMessageOptionByName("test.v1.message_option");

    Collection<Descriptors.Descriptor> options =
        domain.findDescriptorsByOption("test.v1.message_option");
    Assert.assertEquals(1, options.size());

    Descriptors.Descriptor descriptor = options.stream().findFirst().get();
    Message field = (Message) descriptor.getOptions().getField(fd);
    Assert.assertEquals("test.v1.TestOption", field.getDescriptorForType().getFullName());
  }
}
