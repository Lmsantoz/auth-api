package com.lucasmarques.authapi.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfiguration {

    public static final String QUEUE_NAME = "user.registered";
    public static final String EXCHANGE_NAME = "auth.exchange";
    public static final String EXCHANGE_AUTH_DLQ = "auth.exchange.dlq";
    public static final String USER_DLQ = "user.registered.dlq";

    @Bean
    public Queue queue() {
        return QueueBuilder
                .durable(QUEUE_NAME)
                .deadLetterExchange(EXCHANGE_AUTH_DLQ)
                .build();
    }

    @Bean
    public Queue queueDlq() {
        return QueueBuilder
                .durable(USER_DLQ)
                .build();
    }

    @Bean
    public FanoutExchange exchangeDlq() {
        return new FanoutExchange(EXCHANGE_AUTH_DLQ);
    }

    @Bean
    public FanoutExchange exchange() {
        return new FanoutExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder
                .bind(queue())
                .to(exchange());
    }

    @Bean
    public Binding bindingDlq() {
        return BindingBuilder
                .bind(queueDlq())
                .to(exchangeDlq());
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
