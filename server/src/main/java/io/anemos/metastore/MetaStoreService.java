package io.anemos.metastore;

import io.anemos.metastore.core.proto.PContainer;
import io.anemos.metastore.core.proto.ProtoToJsonSchema;
import io.anemos.metastore.core.registry.AbstractRegistry;
import io.anemos.metastore.v1alpha1.MetaStoreGrpc;
import io.anemos.metastore.v1alpha1.Metastore;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

public class MetaStoreService extends MetaStoreGrpc.MetaStoreImplBase {
  private MetaStore metaStore;

  MetaStoreService(MetaStore metaStore) {
    this.metaStore = metaStore;
  }

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

  @Override
  public void getJsonSchema(
      Metastore.GetJsonSchemaRequest request,
      StreamObserver<Metastore.GetJsonSchemaResponse> responseObserver) {

    try {
      AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
      PContainer pContainer = registry.get();
      String jsonSchema = ProtoToJsonSchema.convert(pContainer, request.getMessageName());

      responseObserver.onNext(
          Metastore.GetJsonSchemaResponse.newBuilder().setSchema(jsonSchema).build());
      responseObserver.onCompleted();
    } catch (StatusException | StatusRuntimeException e) {
      responseObserver.onError(e);
    }
  }
}
