package io.anemos.metastore;

import io.anemos.metastore.v1alpha1.MetaStoreGrpc;
import io.anemos.metastore.v1alpha1.Metastore;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;

public class MetaStoreService extends MetaStoreGrpc.MetaStoreImplBase {

  MetaStoreService(MetaStore metaStore) {}

  @Override
  public void getAvroSchema(
      Metastore.GetAvroSchemaRequest request,
      StreamObserver<Metastore.GetAvroSchemaResponse> responseObserver) {
    if ("demo".equals(request.getRegistryName())) {

      responseObserver.onNext(Metastore.GetAvroSchemaResponse.newBuilder().build());
      responseObserver.onCompleted();

    } else {
      responseObserver.onError(new StatusException(Status.NOT_FOUND));
    }
  }
}
