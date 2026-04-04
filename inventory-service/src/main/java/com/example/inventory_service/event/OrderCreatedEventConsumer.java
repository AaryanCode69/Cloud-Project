package com.example.inventory_service.event;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "feature", name = "servicebus-enabled", havingValue = "true")
public class OrderCreatedEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventConsumer.class);

    public void onMessage(ServiceBusReceivedMessageContext context) {
        String payload = context.getMessage().getBody() == null
                ? ""
                : context.getMessage().getBody().toString();

        log.info(
                "Consumed OrderCreatedEvent messageId={} subject={} payload={}",
                context.getMessage().getMessageId(),
                context.getMessage().getSubject(),
                payload
        );
    }

    public void onError(ServiceBusErrorContext errorContext) {
        log.error(
                "Service Bus consumer error. namespace={} entity={} source={} error={}",
                errorContext.getFullyQualifiedNamespace(),
                errorContext.getEntityPath(),
                errorContext.getErrorSource(),
                errorContext.getException().getMessage(),
                errorContext.getException()
        );
    }
}

