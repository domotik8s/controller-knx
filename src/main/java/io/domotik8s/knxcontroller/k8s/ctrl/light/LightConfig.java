package io.domotik8s.knxcontroller.k8s.ctrl.light;

import io.domotik8s.knxcontroller.k8s.ctrl.light.model.V1Beta1KnxLight;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
public class LightConfig {

    private Logger logger = LoggerFactory.getLogger(LightConfig.class);

    @Bean
    public Controller lightController(SharedInformerFactory informerFactory, LightReconciler reconciler, SharedIndexInformer<V1Beta1KnxLight> lightInformer) {
        return ControllerBuilder
                .defaultBuilder(informerFactory)
                .watch(workQueue -> ControllerBuilder
                        .controllerWatchBuilder(V1Beta1KnxLight.class, workQueue)
                        .withResyncPeriod(Duration.of(1, ChronoUnit.SECONDS))
                        .build())
                .withWorkerCount(2)
                .withReconciler(reconciler)
                .withReadyFunc(lightInformer::hasSynced)
                .withName("Light Controller")
                .build();
    }

}
