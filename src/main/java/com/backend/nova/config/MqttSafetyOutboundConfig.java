package com.backend.nova.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.util.UUID;

@Configuration
public class MqttSafetyOutboundConfig {

    @Value("${spring.mqtt.client-id}")
    private String clientId;

    @Bean
    public MessageChannel mqttSafetyOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttSafetyOutboundChannel")
    public MessageHandler mqttSafetyOutbound(MqttPahoClientFactory mqttPahoClientFactory) {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(
                clientId + "_safety_pub_" + UUID.randomUUID(),
                mqttPahoClientFactory
        );
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic("hdc/frontend/safety/update");
        messageHandler.setDefaultQos(2);
        return messageHandler;
    }
}
