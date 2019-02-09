package io.anemos.metastore.core.proto;


import com.google.protobuf.DescriptorProtos;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;

public class ProtoDiffTest {

    @Test
    public void xxx() throws IOException {
        String sd1 = "../tmp/test1.pb";
        DescriptorProtos.FileDescriptorSet fds1 = DescriptorProtos.FileDescriptorSet.parseFrom(new FileInputStream(sd1));
        //Map<String, DescriptorProtos.FileDescriptorProto> fileDescriptorProtoMap = Convert.extractProtoMap(fds1);
        DescriptorProtos.FileDescriptorProto fd1 = fds1.getFileList().get(0);

        String sd2 = "../tmp/test2.pb";
        DescriptorProtos.FileDescriptorSet fds2 = DescriptorProtos.FileDescriptorSet.parseFrom(new FileInputStream(sd2));
        DescriptorProtos.FileDescriptorProto fd2 = fds2.getFileList().get(0);

        new ProtoDiff(fd1, fd2).diff();
    }
}
