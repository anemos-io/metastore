package io.anemos.metastore.core.proto;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import io.anemos.Options;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ProtoDescriptorTest {

    @Test
    public void optionsTest() throws Exception {
        ProtoDescriptor protoDescriptor = new ProtoDescriptor("src/test/proto/test.pb");
        Descriptors.Descriptor descriptor = protoDescriptor.getDescriptorByName("io.anemos.protobeam.examples.ToFlatten");
        Descriptors.FieldDescriptor fieldDescriptor = descriptor.findFieldByName("meta");
//
//        ExtensionRegistry registry = ExtensionRegistry.newInstance();
//        Annotations.registerAllExtensions(registry);
//        Descriptors.FileDescriptor.internalUpdateFileDescriptor(descriptor.getFile(), registry);
//
        DescriptorProtos.FieldOptions options = fieldDescriptor.getOptions();

        Assert.assertTrue(options.hasExtension(Options.fieldRewrite));

    }
}
