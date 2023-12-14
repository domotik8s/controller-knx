package io.domotik8s.knxcontroller.k8s.config;

import io.domotik8s.knxcontroller.k8s.reconciler.StringPropertyReconciler;
import io.domotik8s.model.generic.PropertyList;
import io.domotik8s.model.knx.KnxStringProperty;
import io.domotik8s.model.knx.KnxStringPropertyList;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static io.domotik8s.knxcontroller.k8s.Constants.*;

// @Configuration
public class StringPropertyConfig {

    private Logger logger = LoggerFactory.getLogger(StringPropertyConfig.class);

    @Bean("stringPropertyClient")
    public GenericKubernetesApi<KnxStringProperty, PropertyList> stringPropertyClient(ApiClient client) {
        return new GenericKubernetesApi(
                KnxStringProperty.class, KnxStringPropertyList.class,
                API_GROUP, API_VERSION, "stringproperties",
                client
        );
    }

    @Bean("stringPropertyInformer")
    public SharedIndexInformer<KnxStringProperty> stringPropertyInformer(SharedInformerFactory informerFactory, @Qualifier("stringPropertyClient")  GenericKubernetesApi<KnxStringProperty, KnxStringPropertyList> stringPropertyClient) {
        return informerFactory.sharedIndexInformerFor(stringPropertyClient, KnxStringProperty.class, 0);
    }

    @Bean("stringPropertyController")
    public Controller stringPropertyController(SharedInformerFactory informerFactory, StringPropertyReconciler reconciler, @Qualifier("stringPropertyInformer") SharedIndexInformer<KnxStringProperty> stringPropertyInformer, SystemInstanceFilter filter) {
        return ControllerBuilder
                .defaultBuilder(informerFactory)
                .watch(workQueue -> ControllerBuilder
                        .controllerWatchBuilder(KnxStringProperty.class, workQueue)
                        .withResyncPeriod(Duration.of(1, ChronoUnit.SECONDS))
                        .withOnAddFilter(filter::test)
                        .withOnUpdateFilter((before, after) -> filter.test(after))
                        .withOnDeleteFilter((res, flag) -> filter.test(res))
                        .build())
                .withWorkerCount(1)
                .withReconciler(reconciler)
                .withReadyFunc(stringPropertyInformer::hasSynced)
                .withName("StringProperty Controller")
                .build();
    }

}
