/**
 * Copyright 2020 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.confluent.examples.clients.cloud;

import io.confluent.examples.clients.cloud.model.DataRecord;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.errors.TopicExistsException;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static io.confluent.examples.clients.cloud.Util.loadConfig;

public class ProducerExample {

  // Create topic in Confluent Cloud
  public static void createTopic(final String topic,
                          final Properties cloudConfig) {
      final NewTopic newTopic = new NewTopic(topic, Optional.empty(), Optional.empty());
      try (final AdminClient adminClient = AdminClient.create(cloudConfig)) {
          adminClient.createTopics(Collections.singletonList(newTopic)).all().get();
      } catch (final InterruptedException | ExecutionException e) {
          // Ignore if TopicExistsException, which may be valid if topic exists
          if (!(e.getCause() instanceof TopicExistsException)) {
              throw new RuntimeException(e);
          }
      }
  }

  public static void main(final String[] args) throws IOException {
    if (args.length != 2) {
      System.out.println("Please provide command line arguments: configPath topic");
      System.exit(1);
    }

    // Load properties from a local configuration file
    // Create the configuration file (e.g. at '$HOME/.confluent/java.config') with configuration parameters
    // to connect to your Kafka cluster, which can be on your local host, Confluent Cloud, or any other cluster.
    // Follow these instructions to create this file: https://docs.confluent.io/platform/current/tutorials/examples/clients/docs/java.html
    final Properties props = loadConfig(args[0]);

    // Create topic if needed
    final String topic = args[1];
    createTopic(topic, props);

    // Add additional properties.
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaJsonSerializer");

    Producer<String, DataRecord> producer = new KafkaProducer<String, DataRecord>(props);

    // Produce sample data
    final Long numMessages = 10L;
    for (Long i = 0L; i < numMessages; i++) {
      String key = "alice";
      DataRecord record = new DataRecord(i);

      System.out.printf("Producing record: %s\t%s%n", key, record);
      producer.send(new ProducerRecord<String, DataRecord>(topic, key, record), new Callback() {
          @Override
          public void onCompletion(RecordMetadata m, Exception e) {
            if (e != null) {
              e.printStackTrace();
            } else {
              System.out.printf("Produced record to topic %s partition [%d] @ offset %d%n", m.topic(), m.partition(), m.offset());
            }
          }
      });
    }

    producer.flush();

    System.out.printf("10 messages were produced to topic %s%n", topic);

    producer.close();
  }
}
