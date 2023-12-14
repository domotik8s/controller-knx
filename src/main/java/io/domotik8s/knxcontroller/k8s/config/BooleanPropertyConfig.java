package io.domotik8s.knxcontroller.k8s.config;

import io.domotik8s.knxcontroller.k8s.reconciler.BooleanPropertyReconciler;
import io.domotik8s.model.generic.PropertyList;
import io.domotik8s.model.knx.KnxBooleanProperty;
import io.domotik8s.model.knx.KnxBooleanPropertyList;
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

@Configuration
public class BooleanPropertyConfig {

    private Logger logger = LoggerFactory.getLogger(BooleanPropertyConfig.class);

    @Bean("booleanPropertyClient")
    public GenericKubernetesApi<KnxBooleanProperty, KnxBooleanPropertyList> booleanPropertyClient(ApiClient client) {
        return new GenericKubernetesApi(
                KnxBooleanProperty.class, KnxBooleanPropertyList.class,
                API_GROUP, API_VERSION, "booleanproperties",
                client
        );
    }

    @Bean("booleanPropertyInformer")
    public SharedIndexInformer<KnxBooleanProperty> booleanPropertyInformer(SharedInformerFactory informerFactory, @Qualifier("booleanPropertyClient")  GenericKubernetesApi<KnxBooleanProperty, KnxBooleanPropertyList> booleanPropertyClient) {
        return informerFactory.sharedIndexInformerFor(booleanPropertyClient, KnxBooleanProperty.class, 0);
    }

    @Bean("booleanPropertyController")
    public Controller booleanPropertyController(SharedInformerFactory informerFactory, BooleanPropertyReconciler reconciler, @Qualifier("booleanPropertyInformer") SharedIndexInformer<KnxBooleanProperty> booleanPropertyInformer, SystemInstanceFilter filter) {
        return ControllerBuilder
                .defaultBuilder(informerFactory)
                .watch(workQueue -> ControllerBuilder
                        .controllerWatchBuilder(KnxBooleanProperty.class, workQueue)
                        .withResyncPeriod(Duration.of(1, ChronoUnit.SECONDS))
                        .withOnAddFilter(filter::test)
                        .withOnUpdateFilter((before, after) -> filter.test(after))
                        .withOnDeleteFilter((res, flag) -> filter.test(res))
                        .build())
                .withWorkerCount(1)
                .withReconciler(reconciler)
                .withReadyFunc(booleanPropertyInformer::hasSynced)
                .withName("BooleanProperty Controller")
                .build();
    }

}
