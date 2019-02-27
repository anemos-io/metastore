package io.anemos.metastore;

import com.google.protobuf.DescriptorProtos;
import io.anemos.metastore.core.proto.ProtoDescriptor;
import io.anemos.metastore.core.proto.validate.ProtoDiff;
import io.anemos.metastore.core.proto.validate.ValidationResults;
import io.anemos.metastore.v1alpha1.SchemaRegistyServiceGrpc;
import io.anemos.metastore.v1alpha1.Schemaregistry;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

public class SchemaRegistryService extends SchemaRegistyServiceGrpc.SchemaRegistyServiceImplBase {

    MetaStore metaStore;

    public SchemaRegistryService(MetaStore metaStore) {
        this.metaStore = metaStore;
    }

    @Override
    public void submitSchema(Schemaregistry.SubmitSchemaRequest request, StreamObserver<Schemaregistry.SubmitSchemaResponse> responseObserver) {
        DescriptorProtos.FileDescriptorSet fileDescriptorProto = null;
//        try {
//            fileDescriptorProto = DescriptorProtos.FileDescriptorSet.parseFrom(request.getFdProtoSet());
//        } catch (InvalidProtocolBufferException e) {
//            e.printStackTrace();
//        }
        ProtoDescriptor in = null;
        try {
            in = new ProtoDescriptor(request.getFdProtoSet().newInput());
        } catch (IOException e) {
            e.printStackTrace();
        }

//        Map<String, Descriptors.FileDescriptor> fileDescriptorMap = Convert.convertFileDescriptorSet(fileDescriptorProto);
//
//        fileDescriptorMap.forEach(
//                (k, v) -> {
//                    System.out.println(k);
//                });

        ValidationResults results = new ValidationResults();
        ProtoDiff diff = new ProtoDiff(metaStore.test, in, results);
        diff.diffOnFileName("test/v1alpha1/simple.proto");

        responseObserver.onNext(Schemaregistry.SubmitSchemaResponse
                .newBuilder()
                .putResult("", results.getResult())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void verifySchema(Schemaregistry.SubmitSchemaRequest request, StreamObserver<Schemaregistry.SubmitSchemaResponse> responseObserver) {
        super.verifySchema(request, responseObserver);
    }
}
