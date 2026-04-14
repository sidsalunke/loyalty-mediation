package com.loyalty.vendormock.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.vendormock.model.EnrollmentEvent;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publishes enrolment completion events to the Solace topic that the mediation
 * service is subscribed to — simulating what the vendor's loyalty platform would do.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SolaceEventPublisher {

    private static final String TOPIC_NAME = "loyalty/enrolment/status";

    private final XMLMessageProducer producer;
    private final ObjectMapper objectMapper;

    public void publish(EnrollmentEvent event) {
        if (producer == null) {
            log.warn("Solace producer not available — skipping event publish [correlationId={}, status={}]",
                    event.getCorrelationId(), event.getStatus());
            return;
        }
        try {
            Topic topic = JCSMPFactory.onlyInstance().createTopic(TOPIC_NAME);
            TextMessage message = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            message.setText(objectMapper.writeValueAsString(event));
            message.setDeliveryMode(com.solacesystems.jcsmp.DeliveryMode.DIRECT);

            producer.send(message, topic);
            log.info("Published enrolment event to Solace [topic={}, correlationId={}, status={}]",
                    TOPIC_NAME, event.getCorrelationId(), event.getStatus());

        } catch (JCSMPException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.error("Failed to publish enrolment event to Solace [correlationId={}]",
                    event.getCorrelationId(), ex);
        }
    }
}
