package com.loyalty.mediation.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Emits structured log events in Splunk HEC (HTTP Event Collector) JSON format.
 *
 * In production this would be shipped to Splunk via a log forwarder or HEC endpoint.
 * Locally the events are written to stdout — Splunk picks them up via the Universal
 * Forwarder or a Docker log driver.
 *
 * Splunk HEC envelope:
 * {
 *   "time":       <epoch seconds>,
 *   "sourcetype": "loyalty:mediation",
 *   "event": {
 *     "level":       "INFO|WARN|ERROR",
 *     "eventType":   "<ENROLMENT_ACCEPTED | ENROLMENT_COMPLETED | ENROLMENT_ERROR | ...>",
 *     "correlationId": "...",
 *     ...additional fields...
 *   }
 * }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SplunkLogger {

    private static final String SOURCE_TYPE = "loyalty:mediation";
    private final ObjectMapper objectMapper;

    public void info(String eventType, String correlationId, Map<String, Object> fields) {
        emit("INFO", eventType, correlationId, fields);
    }

    public void warn(String eventType, String correlationId, Map<String, Object> fields) {
        emit("WARN", eventType, correlationId, fields);
    }

    public void error(String eventType, String correlationId, String errorMessage, Map<String, Object> fields) {
        Map<String, Object> enriched = new LinkedHashMap<>(fields);
        enriched.put("errorMessage", errorMessage);
        emit("ERROR", eventType, correlationId, enriched);
    }

    private void emit(String level, String eventType, String correlationId, Map<String, Object> fields) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("level", level);
            event.put("eventType", eventType);
            event.put("correlationId", correlationId);
            event.putAll(fields);

            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("time", Instant.now().getEpochSecond());
            envelope.put("sourcetype", SOURCE_TYPE);
            envelope.put("event", event);

            String json = objectMapper.writeValueAsString(envelope);

            switch (level) {
                case "ERROR" -> log.error("SPLUNK_EVENT {}", json);
                case "WARN"  -> log.warn("SPLUNK_EVENT {}", json);
                default      -> log.info("SPLUNK_EVENT {}", json);
            }
        } catch (Exception ex) {
            log.error("Failed to serialise Splunk event [type={}]", eventType, ex);
        }
    }
}
