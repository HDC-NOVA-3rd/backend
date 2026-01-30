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
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.UUID;

@Configuration
public class MqttSafetySensorSubConfig {

    @Value("${spring.mqtt.client-id}")
    private String clientId;
    @Value("${spring.mqtt.topic.safety}")
    private String safetyTopic;

    @Bean
    public MessageChannel mqttSafetyInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttSafetyInboundAdapter(MqttPahoClientFactory mqttPahoClientFactory,
                                                    @Qualifier("mqttSafetyInputChannel") MessageChannel mqttSafetyInputChannel) {
        if (safetyTopic == null || safetyTopic.isBlank()) {
            throw new IllegalStateException("MQTT safety subscription topic is empty");
        }
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + "_safety_sub_" + UUID.randomUUID(),
                        mqttPahoClientFactory,
                        safetyTopic);

        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(2);
        adapter.setOutputChannel(mqttSafetyInputChannel);
        return adapter;
    }
}
