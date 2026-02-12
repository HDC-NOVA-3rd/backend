package com.backend.nova.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
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

    //DirectChannel = 구독자(= @ServiceActivator) 1명만 안정적으로 사용 가능
    //PublishSubscribeChannel = 구독자 여러 명 동시 수신 가능
    @Bean
    public MessageChannel mqttEnvInputChannel() {
        // return new DirectChannel();
        return new PublishSubscribeChannel();
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

        adapter.setCompletionTimeout(30000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttEnvInputChannel);
        return adapter;
    }
}
