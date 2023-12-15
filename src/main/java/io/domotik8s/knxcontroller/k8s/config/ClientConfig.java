package io.domotik8s.knxcontroller.k8s.config;

import io.domotik8s.model.knx.KnxBooleanProperty;
import io.domotik8s.model.knx.KnxBooleanPropertyList;
import io.domotik8s.model.knx.KnxNumberProperty;
import io.domotik8s.model.knx.KnxNumberPropertyList;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

import static io.domotik8s.knxcontroller.k8s.Constants.API_GROUP;
import static io.domotik8s.knxcontroller.k8s.Constants.API_VERSION;

@Configuration
public class ClientConfig {

    private Logger logger = LoggerFactory.getLogger(ClientConfig.class);

    @Bean
    public ApiClient apiClient() throws IOException {
        ApiClient client = io.kubernetes.client.util.Config.defaultClient();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
        return client;
    }

    @Bean("booleanPropertyClient")
    public GenericKubernetesApi<KnxBooleanProperty, KnxBooleanPropertyList> booleanPropertyClient(ApiClient client) {
        return new GenericKubernetesApi(
                KnxBooleanProperty.class, KnxBooleanPropertyList.class,
                API_GROUP, API_VERSION, "booleanproperties",
                client
        );
    }

    @Bean("numberPropertyClient")
    public GenericKubernetesApi<KnxNumberProperty, KnxNumberPropertyList> numberPropertyClient(ApiClient client) {
        return new GenericKubernetesApi(
                KnxNumberProperty.class, KnxNumberPropertyList.class,
                API_GROUP, API_VERSION, "numberproperties",
                client
        );
    }

}
