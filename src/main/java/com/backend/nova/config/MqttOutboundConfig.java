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
public class MqttOutboundConfig {
    @Value("${spring.mqtt.client-id}")
    private String clientId;

    // 공통으로 publish에 사용하는 발신 채널
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutboundAdapter(MqttPahoClientFactory factory) {
        // 하나의 Client ID로 통합 (예: server_pub)
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(clientId + UUID.randomUUID(), factory);

        handler.setAsync(true);
        handler.setDefaultQos(1);
        // 특정 토픽이 헤더에 없을 때만 사용하는 기본 토픽 (필요 없다면 제거 가능)
        // handler.setDefaultTopic("default/topic");
        return handler;
    }
}
