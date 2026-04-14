package com.loyalty.vendormock.config;

import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishCorrelatingEventHandler;
import com.solacesystems.jcsmp.XMLMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SolaceConfig {

    @Value("${solace.host}")
    private String host;

    @Value("${solace.vpn:default}")
    private String vpn;

    @Value("${solace.username:admin}")
    private String username;

    @Value("${solace.password:admin}")
    private String password;

    /**
     * Returns null if Solace is unreachable so the application still starts.
     * SolaceEventPublisher checks for a null session and logs a warning instead of publishing.
     */
    @Bean
    public JCSMPSession jcsmpSession() {
        try {
            JCSMPProperties properties = new JCSMPProperties();
            properties.setProperty(JCSMPProperties.HOST, host);
            properties.setProperty(JCSMPProperties.VPN_NAME, vpn);
            properties.setProperty(JCSMPProperties.USERNAME, username);
            properties.setProperty(JCSMPProperties.PASSWORD, password);

            JCSMPSession session = JCSMPFactory.onlyInstance().createSession(properties);
            session.connect();
            log.info("Vendor Mock connected to Solace [host={}, vpn={}]", host, vpn);
            return session;
        } catch (JCSMPException ex) {
            log.error("Could not connect to Solace — vendor mock will start but Solace events will not be published. [host={}]", host, ex);
            return null;
        }
    }

    @Bean
    public XMLMessageProducer xmlMessageProducer(JCSMPSession session) throws JCSMPException {
        if (session == null) {
            log.warn("Skipping XMLMessageProducer creation — no Solace session available");
            return null;
        }
        return session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
            @Override
            public void responseReceivedEx(Object correlationKey) {
                log.debug("Solace publish confirmed [correlationKey={}]", correlationKey);
            }

            @Override
            public void handleErrorEx(Object correlationKey, JCSMPException ex, long timestamp) {
                log.error("Solace publish error [correlationKey={}]", correlationKey, ex);
            }
        });
    }
}
