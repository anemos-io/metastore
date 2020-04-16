package io.anemos.metastore.provider;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import io.anemos.metastore.v1alpha1.Report;
import io.opencensus.common.Scope;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GooglePubsub implements EventingProvider {
  private static final Logger LOG = LoggerFactory.getLogger(GooglePubsub.class);
  private static final Tracer TRACER = Tracing.getTracer();
  private Publisher publisherDescriptorChange;
  private Publisher publisherBindingChange;

  @Override
  public void initForChangeEvent(RegistryInfo registryInfo, Map<String, String> config) {
    String project = ServiceOptions.getDefaultProjectId();

    if (config.get("project") == null && project == null) {
      throw new RuntimeException("project variable not set");
    }
    if (config.get("topic_descriptor_change") == null) {
      throw new RuntimeException("topic_descriptor_change variable not set");
    }
    if (config.get("topic_binding_change") == null) {
      throw new RuntimeException("topic variable not set");
    }

    if (config.get("project") != null) {
      project = config.get("project");
    }
    String topicDescriptorChange = config.get("topic_descriptor_change");
    ProjectTopicName projectTopicDescriptorChange =
        ProjectTopicName.of(project, topicDescriptorChange);
    try {
      publisherDescriptorChange = Publisher.newBuilder(projectTopicDescriptorChange).build();
    } catch (IOException e) {
      throw new RuntimeException(
          "Unable to initialize topic_descriptor_change Pubsub Publisher", e);
    }

    String topicBindingChange = config.get("topic_binding_change");
    ProjectTopicName projectTopicBindingChange = ProjectTopicName.of(project, topicBindingChange);
    try {
      publisherBindingChange = Publisher.newBuilder(projectTopicBindingChange).build();
    } catch (IOException e) {
      throw new RuntimeException("Unable to initialize topic_binding_change Pubsub Publisher", e);
    }
  }

  @Override
  public void descriptorsChanged(Report report) {
    try (Scope scope =
        TRACER
            .spanBuilder("GooglePubsub.descriptorsChanged")
            .setRecordEvents(true)
            .startScopedSpan()) {
      PubsubMessage message = PubsubMessage.newBuilder().setData(report.toByteString()).build();
      ApiFuture<String> future = publisherDescriptorChange.publish(message);
      ApiFutures.addCallback(
          future,
          new ApiFutureCallback<String>() {
            @Override
            public void onFailure(Throwable t) {
              LOG.error("Error publishing changes to Pubsub", t);
            }

            @Override
            public void onSuccess(String messageId) {
              LOG.debug("Published changes to Pubsub");
            }
          },
          MoreExecutors.directExecutor());
    }
  }
}
