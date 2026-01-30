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

import java.util.Arrays;
import java.util.UUID;

//subscribe용 설정
@Configuration
public class MqttSensorDataSubConfig {

    @Value("${spring.mqtt.client-id}")
    private String clientId;
    @Value("${spring.mqtt.topic.device}")
    private String deviceTopic;

    //1. 수신채널생성
    @Bean
    public MessageChannel mqttInputChannel(){
        return new DirectChannel();
    }
    //2. mqtt어댑터(브로커 -> 채널 데이터를 이동)
    @Bean
    public MessageProducer inboundAdapter(MqttPahoClientFactory mqttPahoClientFactory){
        //subscribe되어 들어오는 메세지를 받아서 처리하는 객체
        //메세지를 받기 위해서는 브로커에 연결해야 한다. 따라서 매개변수로 브로커에 연결하는 객체를 생성하는 Factory객체를 주입받아서 내부에서 연결을 수행하고 매개변수로 전달한 토픽으로 구독신청을 한다.
        String[] topics = resolveTopics();
        if (topics.length == 0) {
            throw new IllegalStateException("MQTT subscription topics are empty");
        }
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId +"_sub"+ UUID.randomUUID().toString(), mqttPahoClientFactory, topics);

        adapter.setCompletionTimeout(5000); // 5초 연결 대기 후 에러
        //통신은 바이너리로 주고 받는다. 이를 String문자열로 변환해주는 작업을 수행하고 전체 payload로 Message객체를 생성
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(2);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    private String[] resolveTopics() {
        if (deviceTopic == null || deviceTopic.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(deviceTopic.split(","))
                .map(String::trim)
                .filter(topic -> !topic.isBlank())
                .toArray(String[]::new);
    }
}
