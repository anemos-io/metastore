package io.anemos.metastore;

import io.anemos.metastore.v1alpha1.MetaStoreServiceGrpc;
import io.anemos.metastore.v1alpha1.Metastore;
import io.grpc.stub.StreamObserver;

public class MetaStoreService extends MetaStoreServiceGrpc.MetaStoreServiceImplBase {


    @Override
    public void test(Metastore.Emptry request, StreamObserver<Metastore.Emptry> responseObserver) {
        responseObserver.onNext(Metastore.Emptry.newBuilder().build());
        responseObserver.onCompleted();
    }
}
