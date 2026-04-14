package com.loyalty.mediation.config;

import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
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
     * EnrollmentEventListener checks for a null session and skips subscription setup.
     */
    @Bean
    public JCSMPSession jcsmpSession() {
        try {
            JCSMPProperties properties = new JCSMPProperties();
            properties.setProperty(JCSMPProperties.HOST, host);
            properties.setProperty(JCSMPProperties.VPN_NAME, vpn);
            properties.setProperty(JCSMPProperties.USERNAME, username);
            properties.setProperty(JCSMPProperties.PASSWORD, password);
            properties.setProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, true);

            JCSMPSession session = JCSMPFactory.onlyInstance().createSession(properties);
            session.connect();
            log.info("Connected to Solace PubSub+ [host={}, vpn={}]", host, vpn);
            return session;
        } catch (JCSMPException ex) {
            log.error("Could not connect to Solace — mediation will start but enrolment events will not be received. [host={}]", host, ex);
            return null;
        }
    }
}
