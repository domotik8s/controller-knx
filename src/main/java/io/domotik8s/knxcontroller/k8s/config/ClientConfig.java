package io.domotik8s.knxcontroller.k8s.config;

import io.kubernetes.client.openapi.ApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class ClientConfig {

    @Bean
    public ApiClient apiClient() throws IOException {
        ApiClient client = io.kubernetes.client.util.Config.defaultClient();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
        return client;
    }

}
