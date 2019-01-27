package io.anemos.metastore;

import com.google.protobuf.Descriptors;
import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.core.proto.ProtoToAvroSchema;

public class FooBar {

    public static void main(String... a) throws Exception {

        String sd = "/Users/AlexVB/Repos/src/quantum.build/proton/modules/boreporting/_quantum/api/service_descriptor.pb";


        ProtoDescriptor pb = new ProtoDescriptor(sd);



        //pb.writeToDirectory("tmp/");

        String messsageTypeName =  "quantum.api.grpc.CampaignOverviewResponse";
        Descriptors.Descriptor descriptor = pb.getDescriptorByName(messsageTypeName);

        System.out.println(ProtoToAvroSchema.convert(pb,messsageTypeName ));

//        ProtoBeamBasicSpecial.Builder builder = ProtoBeamBasicSpecial.newBuilder();
//        ProtoBeamBasicSpecial build = builder.build();
//
//        Descriptors.Descriptor descriptorForType = build.getDescriptorForType();
//        Descriptors.FileDescriptor fileDescriptor = descriptorForType.getFile();
//
//        ProtoLanguageFileWriter.write(fileDescriptor, System.out);
    }

}
