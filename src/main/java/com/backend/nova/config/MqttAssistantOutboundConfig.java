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
    public class MqttAssistantOutboundConfig {

        @Bean
        public MessageChannel mqttAssistantOutboundChannel() {
            return new DirectChannel();
        }

        @Bean
        @ServiceActivator(inputChannel = "mqttAssistantOutboundChannel")
        public MessageHandler mqttAssistantOutboundAdapter(
                MqttPahoClientFactory mqttPahoClientFactory,
                @Value("${spring.mqtt.client-id}") String clientId
        ) {
            MqttPahoMessageHandler handler =
                    new MqttPahoMessageHandler(clientId + "_assistant_pub_" + UUID.randomUUID(), mqttPahoClientFactory);

            handler.setAsync(true);// 비동기 전송
            handler.setDefaultQos(0);
            handler.setDefaultRetained(false);

            // topic은 Message header(MqttHeaders.TOPIC)로 지정
            return handler;
        }
    }
