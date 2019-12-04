package io.anemos.metastore;

import io.anemos.metastore.core.proto.ProtoToJsonSchema;
import io.anemos.metastore.core.registry.AbstractRegistry;
import io.anemos.metastore.putils.ProtoDomain;
import io.anemos.metastore.v1alpha1.MetaStoreGrpc;
import io.anemos.metastore.v1alpha1.MetaStoreP;
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
      MetaStoreP.GetAvroSchemaRequest request,
      StreamObserver<MetaStoreP.GetAvroSchemaResponse> responseObserver) {
    if ("demo".equals(request.getRegistryName())) {

      responseObserver.onNext(MetaStoreP.GetAvroSchemaResponse.newBuilder().build());
      responseObserver.onCompleted();

    } else {
      responseObserver.onError(new StatusException(Status.NOT_FOUND));
    }
  }

  @Override
  public void getJsonSchema(
      MetaStoreP.GetJsonSchemaRequest request,
      StreamObserver<MetaStoreP.GetJsonSchemaResponse> responseObserver) {

    try {
      AbstractRegistry registry = metaStore.registries.get(request.getRegistryName());
      ProtoDomain pContainer = registry.get();
      String jsonSchema = ProtoToJsonSchema.convert(pContainer, request.getMessageName());

      responseObserver.onNext(
          MetaStoreP.GetJsonSchemaResponse.newBuilder().setSchema(jsonSchema).build());
      responseObserver.onCompleted();
    } catch (StatusException | StatusRuntimeException e) {
      responseObserver.onError(e);
    }
  }
}
