package io.anemos.metastore;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import io.anemos.metastore.core.proto.Convert;
import io.anemos.metastore.core.proto.ProtoLanguageFileWriter;

import java.io.*;
import java.util.Map;

public class FooBar {

    public static void main(String... a) throws Exception {

        String sd = "/Users/AlexVB/Repos/src/quantum.build/proton/modules/boreporting/_quantum/api/service_descriptor.pb";


        DescriptorProtos.FileDescriptorSet fileDescriptorProto = DescriptorProtos.FileDescriptorSet.parseFrom(new FileInputStream(sd));

        Map<String, Descriptors.FileDescriptor> fileDescriptorMap = Convert.convertFileDescriptorSet(fileDescriptorProto);

        fileDescriptorMap.forEach(
                (k, v) -> {
                    File file = new File("tmp/" + k);
                    file.getParentFile().mkdirs();
                    try (OutputStream out = new FileOutputStream(file)) {
                        ProtoLanguageFileWriter.write(v, out);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    System.out.println(k);
                    //ProtoLanguageFileWriter.write(v, System.out);
                }

        );

//        ProtoBeamBasicSpecial.Builder builder = ProtoBeamBasicSpecial.newBuilder();
//        ProtoBeamBasicSpecial build = builder.build();
//
//        Descriptors.Descriptor descriptorForType = build.getDescriptorForType();
//        Descriptors.FileDescriptor fileDescriptor = descriptorForType.getFile();
//
//        ProtoLanguageFileWriter.write(fileDescriptor, System.out);
    }

}
