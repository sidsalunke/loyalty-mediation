package com.loyalty.mediation.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.mediation.logging.SplunkLogger;
import com.loyalty.mediation.model.EnrollmentEvent;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Subscribes to the Solace topic on which the vendor publishes enrolment completion events.
 *
 * Topic: loyalty/enrolment/status
 *
 * On SUCCESS: logs membership number and tier assignment.
 * On FAILURE: logs to Splunk as an error event.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentEventListener {

    private static final String TOPIC_NAME = "loyalty/enrolment/status";

    private final JCSMPSession session;
    private final ObjectMapper objectMapper;
    private final SplunkLogger splunkLogger;

    @Value("${solace.subscription.enabled:true}")
    private boolean subscriptionEnabled;

    private XMLMessageConsumer consumer;

    @PostConstruct
    public void startListening() throws JCSMPException {
        if (!subscriptionEnabled) {
            log.info("Solace subscription disabled — skipping listener setup");
            return;
        }
        if (session == null) {
            log.warn("No Solace session available — skipping listener setup");
            return;
        }

        Topic topic = JCSMPFactory.onlyInstance().createTopic(TOPIC_NAME);

        consumer = session.getMessageConsumer(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
                handleMessage(msg);
            }

            @Override
            public void onException(JCSMPException ex) {
                log.error("Solace consumer exception on topic [{}]", TOPIC_NAME, ex);
                splunkLogger.error("SOLACE_CONSUMER_ERROR", "n/a", ex.getMessage(), Map.of("topic", TOPIC_NAME));
            }
        });

        session.addSubscription(topic);
        consumer.start();
        log.info("Listening for enrolment events on Solace topic [{}]", TOPIC_NAME);
    }

    @PreDestroy
    public void stopListening() {
        if (consumer != null) {
            consumer.close();
            log.info("Solace consumer closed");
        }
    }

    private void handleMessage(BytesXMLMessage msg) {
        try {
            String payload = msg instanceof TextMessage tm
                    ? tm.getText()
                    : new String(msg.getBytes());

            log.debug("Received Solace message [topic={}, payload={}]", TOPIC_NAME, payload);

            EnrollmentEvent event = objectMapper.readValue(payload, EnrollmentEvent.class);
            processEvent(event);

        } catch (Exception ex) {
            log.error("Failed to process Solace message from topic [{}]", TOPIC_NAME, ex);
            splunkLogger.error("ENROLMENT_EVENT_PARSE_ERROR", "unknown", ex.getMessage(),
                    Map.of("topic", TOPIC_NAME));
        }
    }

    private void processEvent(EnrollmentEvent event) {
        if ("success".equalsIgnoreCase(event.getStatus())) {
            log.info("Enrolment completed [correlationId={}, membershipNumber={}, tier={}]",
                    event.getCorrelationId(), event.getMembershipNumber(), event.getTier());

            splunkLogger.info(
                    "ENROLMENT_COMPLETED",
                    event.getCorrelationId(),
                    Map.of(
                            "membershipNumber", event.getMembershipNumber(),
                            "tier", event.getTier(),
                            "status", "COMPLETED"
                    )
            );

        } else {
            log.error("Enrolment failed according to vendor event [correlationId={}, error={}]",
                    event.getCorrelationId(), event.getErrorMessage());

            splunkLogger.error(
                    "ENROLMENT_FAILED",
                    event.getCorrelationId(),
                    event.getErrorMessage(),
                    Map.of("status", "FAILED")
            );
        }
    }
}
