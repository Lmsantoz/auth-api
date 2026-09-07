package com.lucasmarques.authapi.consumer;

import com.lucasmarques.authapi.config.RabbitMQConfiguration;
import com.lucasmarques.authapi.dto.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitConsumer {

    @RabbitListener(queues = RabbitMQConfiguration.QUEUE_NAME)
    public void handleUserRegistered(UserRegisteredEvent userRegisteredEvent) {
        log.info("Processando envio de e-mail de boas-vindas para o usuário {}", userRegisteredEvent.id());
    }

}
