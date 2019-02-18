package io.anemos.metastore.core.proto;


import com.google.protobuf.DescriptorProtos;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;

public class TreeShakeTest {

    @Test
    public void xxx() throws IOException {
        String sd1 = "../tmp/test1.pb";
        DescriptorProtos.FileDescriptorSet fds1 = DescriptorProtos.FileDescriptorSet.parseFrom(new FileInputStream(sd1));
        //Map<String, DescriptorProtos.FileDescriptorProto> fileDescriptorProtoMap = Convert.extractProtoMap(fds1);
        DescriptorProtos.FileDescriptorProto fd1 = fds1.getFileList().get(0);

        String sd2 = "../tmp/test2.pb";
        DescriptorProtos.FileDescriptorSet fds2 = DescriptorProtos.FileDescriptorSet.parseFrom(new FileInputStream(sd2));
        DescriptorProtos.FileDescriptorProto fd2 = fds2.getFileList().get(0);

        ProtoDescriptor d1 = new ProtoDescriptor(sd1);
        ProtoDescriptor d2 = new ProtoDescriptor(sd2);

        ValidationResults results = new ValidationResults();

        new ProtoDiff(d1, d2, results).diffOnFileName("test/v1alpha1/simple.proto");

        System.out.println(results.getResult());

    }
}
