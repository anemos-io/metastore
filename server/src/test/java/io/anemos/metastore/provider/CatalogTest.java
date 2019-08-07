package io.anemos.metastore.provider;

import com.google.cloud.datacatalog.ColumnSchema;
import com.google.cloud.datacatalog.Entry;
import com.google.cloud.datacatalog.LookupEntryRequest;
import com.google.cloud.datacatalog.Schema;
import com.google.cloud.datacatalog.TagTemplate;
import com.google.cloud.datacatalog.UpdateEntryRequest;
import com.google.cloud.datacatalog.v1beta1.DataCatalogClient;
import com.google.protobuf.FieldMask;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CatalogTest {

  @Ignore
  @Test
  public void experiments() throws Exception {

    try (DataCatalogClient dataCatalogClient = DataCatalogClient.create()) {
      //           System.out.println(dataCatalogClient.lookupEntry(LookupEntryRequest.newBuilder()
      //
      // .setLinkedResource("//pubsub.googleapis.com/projects/vptech-data-core/topics/agora_perf")
      //                    .build()));
      //            dataCatalogClient.createTag(
      //
      // CreateTagRequest.newBuilder().setTag(Tag.newBuilder().setName("xxx").build()).build());

      TagTemplate tagTemplate =
          dataCatalogClient.getTagTemplate(
              "projects/vptech-data-north/locations/us-central1/tagTemplates/metastore");
      System.out.println(tagTemplate);

      Entry entry =
          dataCatalogClient.lookupEntry(
              LookupEntryRequest.newBuilder()
                  .setLinkedResource(
                      "//pubsub.googleapis.com/projects/vptech-data-core/topics/agora_perf")
                  .build());
      System.out.println(entry);

      Entry updatedEntry =
          Entry.newBuilder(entry)
              .setSchema(
                  Schema.newBuilder()
                      .addColumns(
                          ColumnSchema.newBuilder()
                              .setColumn("x")
                              .setType("int32")
                              .setMode("REPEATED")
                              .setDescription("Cool desc."))
                      .addColumns(ColumnSchema.newBuilder().setColumn("y").setType("string"))
                      .addColumns(
                          ColumnSchema.newBuilder()
                              .setColumn("z")
                              .setType("Timestamp")
                              .addSubcolumns(
                                  ColumnSchema.newBuilder()
                                      .setColumn("nano")
                                      .setType("int32")
                                      .setMode("NULLABLE"))
                              .addSubcolumns(
                                  ColumnSchema.newBuilder().setColumn("seconds").setType("int64"))))
              .build();

      System.out.println(
          dataCatalogClient.updateEntry(
              UpdateEntryRequest.newBuilder()
                  .setUpdateMask(FieldMask.newBuilder().addPaths("schema").build())
                  .setEntry(updatedEntry)
                  .build()));

      //            System.out.println(dataCatalogClient.createTag(CreateTagRequest.newBuilder()
      //                    .setParent(entry.getName())
      //                    .setTag(Tag.newBuilder()
      //                            .setTemplate(tagTemplate.getName())
      //                            .putFields("protobuf_file", TagField.newBuilder()
      //                                    .setStringValue("Foobar").build())
      //                            .putFields("protobuf_schema", TagField.newBuilder()
      //                                    .setStringValue("bar").build())
      //                            .setColumn("y")
      //                            .build())
      //                    .build()));
    }
  }
}
