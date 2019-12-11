package io.anemos.metastore.putils;

import com.google.protobuf.ByteString;
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
}
