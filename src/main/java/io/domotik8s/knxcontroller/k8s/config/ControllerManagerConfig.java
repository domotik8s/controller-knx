package io.domotik8s.knxcontroller.k8s.config;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.ControllerManager;
import io.kubernetes.client.extended.controller.LeaderElectingController;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.leaderelection.LeaderElectionConfig;
import io.kubernetes.client.extended.leaderelection.LeaderElector;
import io.kubernetes.client.extended.leaderelection.resourcelock.EndpointsLock;
import io.kubernetes.client.informer.SharedInformerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ControllerManagerConfig {

    @Bean
    public ControllerManager controllerManager(
            SharedInformerFactory informerFactory,
            @Qualifier("booleanPropertyController") Controller booleanPropertyController,
            @Qualifier("numberPropertyController") Controller numberPropertyController
        ) {

        ControllerManager controllerManager =
                ControllerBuilder.controllerManagerBuilder(informerFactory)
                        .addController(booleanPropertyController)
                        .addController(numberPropertyController)
                        .build();

        return controllerManager;
    }

    @Bean
    public LeaderElectingController leaderController(ControllerManager controllerManager) {
        LeaderElectingController leaderElectingController =
                new LeaderElectingController(
                        new LeaderElector(
                                new LeaderElectionConfig(
                                        new EndpointsLock("kube-system", "leader-election", "foo"),
                                        Duration.ofMillis(10000),
                                        Duration.ofMillis(8000),
                                        Duration.ofMillis(5000))),
                        controllerManager);
        return leaderElectingController;
    }


}
