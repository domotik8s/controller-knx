package io.domotik8s.knxcontroller.k8s.config;

import io.domotik8s.knxcontroller.k8s.reconciler.NumberPropertyReconciler;
import io.domotik8s.knxcontroller.k8s.reconciler.StringPropertyReconciler;
import io.domotik8s.model.generic.PropertyList;
import io.domotik8s.model.knx.KnxNumberProperty;
import io.domotik8s.model.knx.KnxNumberPropertyList;
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

import static io.domotik8s.knxcontroller.k8s.Constants.API_GROUP;
import static io.domotik8s.knxcontroller.k8s.Constants.API_VERSION;

@Configuration
public class NumberPropertyConfig {

    private Logger logger = LoggerFactory.getLogger(NumberPropertyConfig.class);

    @Bean("numberPropertyClient")
    public GenericKubernetesApi<KnxNumberProperty, KnxNumberPropertyList> numberPropertyClient(ApiClient client) {
        return new GenericKubernetesApi(
                KnxNumberProperty.class, KnxNumberPropertyList.class,
                API_GROUP, API_VERSION, "numberproperties",
                client
        );
    }

    @Bean("numberPropertyInformer")
    public SharedIndexInformer<KnxNumberProperty> numberPropertyInformer(
            SharedInformerFactory informerFactory,
            @Qualifier("numberPropertyClient") GenericKubernetesApi<KnxNumberProperty, KnxNumberPropertyList> numberPropertyClient
    ) {
        return informerFactory.sharedIndexInformerFor(numberPropertyClient, KnxNumberProperty.class, 0);
    }

    @Bean("numberPropertyController")
    public Controller numberPropertyController(
            SharedInformerFactory informerFactory,
            NumberPropertyReconciler reconciler,
            @Qualifier("numberPropertyInformer") SharedIndexInformer<KnxNumberProperty> numberPropertyInformer,
            SystemInstanceFilter filter
    ) {
        return ControllerBuilder
                .defaultBuilder(informerFactory)
                .watch(workQueue -> ControllerBuilder
                        .controllerWatchBuilder(KnxNumberProperty.class, workQueue)
                        .withResyncPeriod(Duration.of(1, ChronoUnit.SECONDS))
                        .withOnAddFilter(filter::test)
                        .withOnUpdateFilter((before, after) -> filter.test(after))
                        .withOnDeleteFilter((res, flag) -> filter.test(res))
                        .build())
                .withWorkerCount(1)
                .withReconciler(reconciler)
                .withReadyFunc(numberPropertyInformer::hasSynced)
                .withName("NumberProperty Controller")
                .build();
    }

}
