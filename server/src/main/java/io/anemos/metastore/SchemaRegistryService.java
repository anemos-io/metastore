package io.anemos.metastore;

import com.google.protobuf.ByteString;
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
        ProtoDescriptor in = null;
        try {
            in = new ProtoDescriptor(request.getFdProtoSet().newInput());
        } catch (IOException e) {
            e.printStackTrace();
        }

        metaStore.repo = in;
        metaStore.write();


        responseObserver.onNext(Schemaregistry.SubmitSchemaResponse
                .newBuilder()
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void verifySchema(Schemaregistry.SubmitSchemaRequest request, StreamObserver<Schemaregistry.SubmitSchemaResponse> responseObserver) {
        DescriptorProtos.FileDescriptorSet fileDescriptorProto = null;

        ProtoDescriptor in = null;
        try {
            in = new ProtoDescriptor(request.getFdProtoSet().newInput());
        } catch (IOException e) {
            e.printStackTrace();
        }


        ValidationResults results = new ValidationResults();
        ProtoDiff diff = new ProtoDiff(metaStore.repo, in, results);

        request.getScopeList().forEach(scope -> {
            switch (scope.getEntityScopeCase()) {
                case FILE_NAME:
                    diff.diffOnFileName(scope.getFileName());
                    break;
                case MESSAGE_NAME:
                    break;
                case SERVICE_NAME:
                    break;
                case ENUM_NAME:
                    break;
                default:
                    diff.diffOnPackagePrefix(scope.getPackagePrefix());
            }

//            String messageName = scope.;
//            diff.diffOnFileName();
        });


        responseObserver.onNext(Schemaregistry.SubmitSchemaResponse
                .newBuilder()
                .setReport(results.getReport())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getSchema(Schemaregistry.GetSchemaRequest request, StreamObserver<Schemaregistry.GetSchemaResponse> responseObserver) {
        responseObserver.onNext(Schemaregistry.GetSchemaResponse
                .newBuilder()
                .setFdProtoSet(ByteString.copyFrom(metaStore.repo.toByteArray()))
                .build());
        responseObserver.onCompleted();
    }
}
