package io.domotik8s.knxcontroller.k8s.config;

import io.domotik8s.model.knx.KnxBooleanProperty;
import io.domotik8s.model.knx.KnxBooleanPropertyList;
import io.domotik8s.model.knx.KnxNumberProperty;
import io.domotik8s.model.knx.KnxNumberPropertyList;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InformerConfig {

    private Logger logger = LoggerFactory.getLogger(InformerConfig.class);


    @Bean("booleanPropertyInformer")
    public SharedIndexInformer<KnxBooleanProperty> booleanPropertyInformer(
            SharedInformerFactory informerFactory,
            @Qualifier("booleanPropertyClient") GenericKubernetesApi<KnxBooleanProperty, KnxBooleanPropertyList> booleanPropertyClient
    ) {
        return informerFactory.sharedIndexInformerFor(booleanPropertyClient, KnxBooleanProperty.class, 0);
    }

    @Bean("numberPropertyInformer")
    public SharedIndexInformer<KnxNumberProperty> numberPropertyInformer(
            SharedInformerFactory informerFactory,
            @Qualifier("numberPropertyClient") GenericKubernetesApi<KnxNumberProperty, KnxNumberPropertyList> numberPropertyClient
    ) {
        return informerFactory.sharedIndexInformerFor(numberPropertyClient, KnxNumberProperty.class, 0);
    }

}
