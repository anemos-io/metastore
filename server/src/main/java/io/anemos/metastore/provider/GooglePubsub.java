package io.anemos.metastore.provider;

import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import io.anemos.metastore.v1alpha1.Report;
import java.io.IOException;
import java.util.Map;

public class GooglePubsub implements EventingProvider {

  Publisher publisherDescriptorChange;
  Publisher publisherBindingChange;

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
    PubsubMessage message = PubsubMessage.newBuilder().setData(report.toByteString()).build();
    publisherDescriptorChange.publish(message);
  }
}
