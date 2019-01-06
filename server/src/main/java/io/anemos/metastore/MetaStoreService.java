package io.anemos.metastore;

import io.grpc.stub.StreamObserver;

public class MetaStoreService extends MetaStoreServiceGrpc.MetaStoreServiceImplBase {


    @Override
    public void test(Metastore.Emptry request, StreamObserver<Metastore.Emptry> responseObserver) {
        responseObserver.onNext(Metastore.Emptry.newBuilder().build());
        responseObserver.onCompleted();
    }
}
