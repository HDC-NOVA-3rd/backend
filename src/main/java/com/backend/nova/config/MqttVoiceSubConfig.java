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

import java.util.UUID;

@Configuration
public class MqttVoiceSubConfig {

    @Value("${spring.mqtt.client-id}")
    private String clientId;

    @Value("${spring.mqtt.topic.voice}")
    private String voiceTopic;

    @Bean
    public MessageChannel mqttVoiceInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttVoiceInboundAdapter(MqttPahoClientFactory mqttPahoClientFactory) {
        if (voiceTopic == null || voiceTopic.isBlank()) {
            throw new IllegalStateException("MQTT voice subscription topic is empty");
        }
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        clientId + "_voice_sub_" + UUID.randomUUID(),
                        mqttPahoClientFactory,
                        voiceTopic);

        adapter.setCompletionTimeout(30000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttVoiceInputChannel());
        return adapter;
    }
}
