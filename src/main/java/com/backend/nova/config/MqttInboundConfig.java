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
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.UUID;

@Configuration
@Slf4j
public class MqttInboundConfig {

    @Value("${spring.mqtt.client-id}")
    private String clientId;

    @Value("${spring.mqtt.topic.safety}") private String safetyTopic;
    @Value("${spring.mqtt.topic.env}") private String envTopic;
    @Value("${spring.mqtt.topic.assistant}") private String assistantTopic;
    @Value("${spring.mqtt.topic.voice}") private String voiceTopic;
    @Value("${spring.mqtt.topic.entrance}") private String entranceTopic;


    // 모든 메시지를 가장 먼저 받는 메인 채널
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    // 3. 통합 어댑터: 한 번에 여러 토픽 구독
    @Bean
    public MessageProducer mqttInboundAdapter(MqttPahoClientFactory factory) {
        String[] topics = {
                safetyTopic,
                envTopic,
                assistantTopic,
                voiceTopic,
                entranceTopic // Topic 추가되면 여기에 이어 작성
        };

        log.info("MQTT Inbound Subscribing to topics: {}", Arrays.toString(topics));

        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + UUID.randomUUID(), factory, topics);

        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel()); // 모든 subscribe 메시지를 이곳으로 단일화
        return adapter;
    }
}