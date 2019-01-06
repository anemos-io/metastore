package io.anemos.metastore;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import io.anemos.metastore.core.proto.Convert;
import io.grpc.stub.StreamObserver;

import java.util.Map;

public class SchemaRegistryService extends SchemaRegistyServiceGrpc.SchemaRegistyServiceImplBase {

    @Override
    public void submitSchema(Schemaregistry.SchemaSubmitRequest request, StreamObserver<Schemaregistry.SchemaSubmitResponse> responseObserver) {
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

        responseObserver.onNext(Schemaregistry.SchemaSubmitResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void verifySchema(Schemaregistry.SchemaSubmitRequest request, StreamObserver<Schemaregistry.SchemaSubmitResponse> responseObserver) {
        super.verifySchema(request, responseObserver);
    }
}
