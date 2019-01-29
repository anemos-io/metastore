package io.anemos.metastore;

import io.anemos.metastore.v1alpha1.MetaStoreServiceGrpc;
import io.anemos.metastore.v1alpha1.Metastore;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;

public class MetaStoreService extends MetaStoreServiceGrpc.MetaStoreServiceImplBase {

    public MetaStoreService(MetaStore metaStore) {

    }

    @Override
    public void getAvroSchema(Metastore.GetAvroSchemaRequest request, StreamObserver<Metastore.GetAvroSchemaResponse> responseObserver) {
        if ("demo".equals(request.getRegistryName())) {

            responseObserver.onNext(Metastore.GetAvroSchemaResponse.newBuilder().build());
            responseObserver.onCompleted();

        } else {
            responseObserver.onError(new StatusException(Status.NOT_FOUND));
        }

    }
}
