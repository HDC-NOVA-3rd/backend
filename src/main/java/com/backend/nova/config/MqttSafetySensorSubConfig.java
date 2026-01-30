package com.backend.nova.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

import java.util.Arrays;
import java.util.UUID;

@Configuration
public class MqttSafetySensorSubConfig {

    @Value("${spring.mqtt.client-id}")
    private String clientId;
    @Value("${spring.mqtt.topic.safety}")
    private String safetyTopic;

    @Bean
    public MessageChannel mqttSensorInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttSafetyInboundAdapter(MqttPahoClientFactory mqttPahoClientFactory,
                                                    MessageChannel mqttSensorInputChannel) {
        String[] topics = resolveTopics(safetyTopic);
        if (topics.length == 0) {
            throw new IllegalStateException("MQTT safety subscription topics are empty");
        }
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + "_safety_sub_" + UUID.randomUUID(),
                        mqttPahoClientFactory,
                        topics);

        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(2);
        adapter.setOutputChannel(mqttSensorInputChannel);
        return adapter;
    }

    private String[] resolveTopics(String topicConfig) {
        if (topicConfig == null || topicConfig.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(topicConfig.split(","))
                .map(String::trim)
                .filter(topic -> !topic.isBlank())
                .toArray(String[]::new);
    }
}
