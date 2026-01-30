package com.backend.nova.config;

import org.springframework.beans.factory.annotation.Qualifier;
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
public class MqttEnvSensorSubConfig {

    @Value("${spring.mqtt.client-id}")
    private String clientId;
    @Value("${spring.mqtt.topic.env}")
    private String envTopic;

    @Bean
    public MessageChannel mqttEnvInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttEnvInboundAdapter(MqttPahoClientFactory mqttPahoClientFactory,
                                                 @Qualifier("mqttEnvInputChannel") MessageChannel mqttEnvInputChannel) {
        if (envTopic == null || envTopic.isBlank()) {
            throw new IllegalStateException("MQTT env subscription topic is empty");
        }
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + "_env_sub_" + UUID.randomUUID(),
                        mqttPahoClientFactory,
                        envTopic);

        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttEnvInputChannel);
        return adapter;
    }
}
