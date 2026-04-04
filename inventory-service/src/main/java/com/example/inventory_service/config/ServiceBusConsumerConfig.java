package com.example.inventory_service.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.example.inventory_service.event.OrderCreatedEventConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "feature", name = "servicebus-enabled", havingValue = "true")
public class ServiceBusConsumerConfig {

    @Bean(initMethod = "start", destroyMethod = "close")
    public ServiceBusProcessorClient orderCreatedProcessorClient(
            OrderCreatedEventConsumer consumer,
            @Value("${servicebus.connection-string}") String connectionString,
            @Value("${servicebus.queue-name}") String queueName
    ) {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .queueName(queueName)
                .processMessage(consumer::onMessage)
                .processError(consumer::onError)
                .buildProcessorClient();
    }
}

