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
public class MqttAssistantResultSubConfig {

    @Value("${spring.mqtt.client-id}")
    private String clientId;
    @Value("${spring.mqtt.topic.assistant}")
    private String assistantTopic;

    @Bean
    public MessageChannel mqttAssistantInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttAssistantInboundAdapter(MqttPahoClientFactory mqttPahoClientFactory) {
        String[] topics = resolveTopics();
        if (topics.length == 0) {
            throw new IllegalStateException("MQTT assistant subscription topics are empty");
        }
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + "_assistant_sub_" + UUID.randomUUID(),
                        mqttPahoClientFactory,
                        topics);

        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttAssistantInputChannel());
        return adapter;
    }

    private String[] resolveTopics() {
        if (assistantTopic == null || assistantTopic.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(assistantTopic.split(","))
                .map(String::trim)
                .filter(topic -> !topic.isBlank())
                .toArray(String[]::new);
    }
}
