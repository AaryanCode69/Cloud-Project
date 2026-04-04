package com.example.order_service.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "feature", name = "servicebus-enabled", havingValue = "true")
public class ServiceBusProducerConfig {

    @Bean(destroyMethod = "close")
    public ServiceBusSenderClient orderCreatedSenderClient(
            @Value("${servicebus.connection-string}") String connectionString,
            @Value("${servicebus.queue-name}") String queueName
    ) {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();
    }
}

