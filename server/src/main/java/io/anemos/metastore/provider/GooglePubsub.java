package io.anemos.metastore.provider;

import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import io.anemos.metastore.v1alpha1.Report;
import java.io.IOException;
import java.util.Map;

public class GooglePubsub implements EventingProvider {

  Publisher publisher;

  @Override
  public void initForChangeEvent(RegistryInfo registryInfo, Map<String, String> config) {
    String project = ServiceOptions.getDefaultProjectId();

    if (config.get("project") == null && project == null) {
      throw new RuntimeException("project variable not set");
    }
    if (config.get("topic") == null) {
      throw new RuntimeException("topic variable not set");
    }

    if (config.get("project") != null) {
      project = config.get("project");
    }
    String topicName = config.get("topic");

    ProjectTopicName projectTopicName = ProjectTopicName.of(project, topicName);
    try {
      publisher = Publisher.newBuilder(projectTopicName).build();
    } catch (IOException e) {
      throw new RuntimeException("Unable to initialize Pubsub Publisher", e);
    }
  }

  @Override
  public void descriptorsChanged(Report report) {
    PubsubMessage message = PubsubMessage.newBuilder().setData(report.toByteString()).build();
    publisher.publish(message);
  }
}
