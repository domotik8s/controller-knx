package io.domotik8s.knxcontroller.knx.client;

import io.domotik8s.knxcontroller.knx.properties.KnxProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KnxClientConfig {

    @Bean
    public KnxClient knxClient(KnxProperties properties) {
        KnxClient client = null;
        if (properties.getConnection().getIp() != null)
            client = new KnxIpClient(properties);
        return client;
    }

}