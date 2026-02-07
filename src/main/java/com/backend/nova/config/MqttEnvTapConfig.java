package com.backend.nova.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.MessageChannel;

@Configuration
public class MqttEnvTapConfig {

    /**
     * 기존 mqttEnvInputChannel을 DirectChannel로 쓰고 있으면 소비자가 1개여야 안전함.
     * 그래서 "복제"용 pub-sub 채널을 하나 만들고,
     * mqttEnvInboundAdapter의 outputChannel을 이 pub-sub 채널로 바꾸는 방식이 가장 깔끔함.
     */
    @Bean
    public MessageChannel mqttEnvPubSubChannel() {
        return new PublishSubscribeChannel();
    }
}
