package com.backend.nova.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MqttCommonConfig {
    @Value("${spring.mqtt.url}")
    private String brokerUrl;

    @Value("${spring.mqtt.username:}")
    private String username;

    @Value("${spring.mqtt.password:}")
    private String password;

    //스프링프레임워크에서 브로커에 접속할 수 있는 객체를 만드는 factory 객체 생성
    @Bean
    public MqttPahoClientFactory mqttPahoClientFactory(){
        log.info("Connecting to MQTT Broker: {}", brokerUrl);
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        // Spring Integration adapters already handle recovery; avoid reconnect race.
        options.setAutomaticReconnect(false);
        options.setCleanSession(true);
        options.setConnectionTimeout(15);
        options.setKeepAliveInterval(30);

        // 인증 적용 (allow_anonymous false일 때를 위함)
        if (username != null && !username.isBlank()) {
            options.setUserName(username);
        }
        if (password != null && !password.isBlank()) {
            options.setPassword(password.toCharArray());
        }

        factory.setConnectionOptions(options);
        return factory;
    }
}
