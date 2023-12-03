package com.baeldung.kafka.consumer;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class KafkaListenerWithoutSpringLiveTest {

    @Container
    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

    static {
        Awaitility.setDefaultTimeout(ofSeconds(1L));
        Awaitility.setDefaultPollInterval(ofMillis(50L));
    }

    @Test
    void test() {
        // given
        String topic = "baeldung.articles.published";
        String bootstrapServers = KAFKA_CONTAINER.getBootstrapServers();
        List<String> consumedMessages = new ArrayList<>();

        // when
        try (CustomKafkaListener listener = new CustomKafkaListener(topic, bootstrapServers)) {
            CompletableFuture.runAsync(() ->
                listener.doForEach(consumedMessages::add).run()
            );
        }
        // and
        publishArticles(topic, asList(
            "Introduction to Kafka",
            "Kotlin for Java Developers",
            "Reactive Spring Boot",
            "Deploying Spring Boot Applications",
            "Spring Security"
        ));

        // then
        await().untilAsserted(() -> assertThat(consumedMessages)
            .containsExactlyInAnyOrder(
                "Introduction to Kafka",
                "Kotlin for Java Developers",
                "Reactive Spring Boot",
                "Deploying Spring Boot Applications",
                "Spring Security"
            ));
    }

    private void publishArticles(String topic, List<String> articles) {
        try (KafkaProducer<String, String> producer = testKafkaProducer()) {
            articles.stream()
                .map(article -> new ProducerRecord<String,String>(topic, article))
                .forEach(producer::send);
        }
    }

    private static KafkaProducer<String, String> testKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

}