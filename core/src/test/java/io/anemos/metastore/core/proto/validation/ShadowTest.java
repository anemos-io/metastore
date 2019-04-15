package io.anemos.metastore.core.proto.validation;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.core.proto.TestSets;
import io.anemos.metastore.core.proto.shadow.ShadowRegistry;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.core.test.TestOption;
import io.anemos.metastore.v1alpha1.FileResult;
import io.anemos.metastore.v1alpha1.MessageResult;
import io.anemos.metastore.v1alpha1.OptionChangeInfo;
import io.anemos.metastore.v1alpha1.Report;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

@RunWith(JUnit4.class)
public class ShadowTest {

    @Test
    public void addMessageOptionDeltaTest() throws Exception {
        ProtoDescriptor baseAddMessageOption = TestSets.baseAddMessageOption();
        ProtoDescriptor base = TestSets.baseKnownOption();

        ValidationResults results = new ValidationResults();
        ProtoDiff diff = new ProtoDiff(base, baseAddMessageOption, results);
        diff.diffOnMessage("test.v1.ProtoBeamBasicMessage");

        Report result = results.getReport();
        System.out.println(result);

        ShadowRegistry shadowRegistry = new ShadowRegistry(base, result);
        shadowRegistry.setDelta(result);
        ProtoDescriptor shadow = shadowRegistry.getShadow();

        Descriptors.Descriptor expectedDescriptor = baseAddMessageOption.getDescriptorByName("test.v1.ProtoBeamBasicMessage");
        Descriptors.Descriptor actualDescriptor = shadow.getDescriptorByName("test.v1.ProtoBeamBasicMessage");
        Assert.assertEquals(expectedDescriptor.toProto(), actualDescriptor.toProto());

    }

    @Test
    public void addFieldOptionDeltaTest() throws Exception {
        ProtoDescriptor baseAddFieldOption = TestSets.baseAddFieldOption();
        ProtoDescriptor base = TestSets.baseKnownOption();

        ValidationResults results = new ValidationResults();
        ProtoDiff diff = new ProtoDiff(base, baseAddFieldOption, results);
        diff.diffOnMessage("test.v1.ProtoBeamBasicMessage");

        Report result = results.getReport();
        System.out.println(result);

        ShadowRegistry shadowRegistry = new ShadowRegistry(base, result);
        shadowRegistry.setDelta(result);
        ProtoDescriptor shadow = shadowRegistry.getShadow();

        Descriptors.Descriptor expectedDescriptor = baseAddFieldOption.getDescriptorByName("test.v1.ProtoBeamBasicMessage");
        Descriptors.Descriptor actualDescriptor = shadow.getDescriptorByName("test.v1.ProtoBeamBasicMessage");
        Assert.assertEquals(expectedDescriptor.toProto(), actualDescriptor.toProto());

    }

    @Test
    public void addFileOptionDeltaTest() throws Exception {
        String fileName = "test/v1/simple.proto";
        ProtoDescriptor baseAddFileOption = TestSets.baseAddFileOption();
        ProtoDescriptor base = TestSets.baseKnownOption();

        ValidationResults results = new ValidationResults();
        ProtoDiff diff = new ProtoDiff(base, baseAddFileOption, results);
        diff.diffOnFileName(fileName);

        Report result = results.getReport();
        System.out.println(result);

        ShadowRegistry shadowRegistry = new ShadowRegistry(base, result);
        shadowRegistry.setDelta(result);
        ProtoDescriptor shadow = shadowRegistry.getShadow();

        Descriptors.FileDescriptor expectedDescriptor = baseAddFileOption.getFileDescriptorByFileName(fileName);
        Descriptors.FileDescriptor actualDescriptor = shadow.getFileDescriptorByFileName(fileName);
        Assert.assertEquals(expectedDescriptor.toProto(), actualDescriptor.toProto());

    }

}
