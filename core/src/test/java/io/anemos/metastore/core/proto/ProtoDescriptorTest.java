package io.anemos.metastore.core.proto;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.UnknownFieldSet;
import io.anemos.Annotations;
import io.anemos.Rewrite;
import io.anemos.protobeam.examples.ToFlatten;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.FileInputStream;
import java.util.List;

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

        Assert.assertTrue(options.hasExtension(Annotations.fieldRewrite));

    }
}
