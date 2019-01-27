package io.anemos.metastore;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import io.anemos.metastore.core.proto.Convert;
import io.anemos.metastore.v1alpha1.SchemaRegistyServiceGrpc;
import io.anemos.metastore.v1alpha1.Schemaregistry;
import io.grpc.stub.StreamObserver;

import java.util.Map;

public class SchemaRegistryService extends SchemaRegistyServiceGrpc.SchemaRegistyServiceImplBase {

    @Override
    public void submitSchema(Schemaregistry.SchemaRequest request, StreamObserver<Schemaregistry.SchemaResponse> responseObserver) {
        DescriptorProtos.FileDescriptorSet fileDescriptorProto = null;
        try {
            fileDescriptorProto = DescriptorProtos.FileDescriptorSet.parseFrom(request.getDescriptorBody());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        Map<String, Descriptors.FileDescriptor> fileDescriptorMap = Convert.convertFileDescriptorSet(fileDescriptorProto);

        fileDescriptorMap.forEach(
                (k, v) -> {
                    System.out.println(k);
                });

        responseObserver.onNext(Schemaregistry.SchemaResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void verifySchema(Schemaregistry.SchemaRequest request, StreamObserver<Schemaregistry.SchemaResponse> responseObserver) {
        super.verifySchema(request, responseObserver);
    }
}
