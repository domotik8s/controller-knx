package io.domotik8s.knxcontroller.k8s.config;

import io.domotik8s.knxcontroller.k8s.reconciler.BooleanPropertyReconciler;
import io.domotik8s.knxcontroller.k8s.reconciler.NumberPropertyReconciler;
import io.domotik8s.model.generic.Property;
import io.domotik8s.model.generic.PropertySpec;
import io.domotik8s.model.generic.PropertySystemAddress;
import io.domotik8s.model.knx.KnxBooleanProperty;
import io.domotik8s.model.knx.KnxNumberProperty;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Predicate;

import static io.domotik8s.knxcontroller.k8s.Constants.*;

@Configuration
public class ControllerConfig {

    private Logger logger = LoggerFactory.getLogger(ControllerConfig.class);

    @Bean("booleanPropertyController")
    public Controller booleanPropertyController(
            SharedInformerFactory informerFactory,
            BooleanPropertyReconciler reconciler,
            @Qualifier("booleanPropertyInformer") SharedIndexInformer<KnxBooleanProperty> booleanPropertyInformer
    ) {
        SystemInstanceFilter filter = new SystemInstanceFilter();
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
                .withName("BooleanPropertyController")
                .build();
    }

    @Bean("numberPropertyController")
    public Controller numberPropertyController(
            SharedInformerFactory informerFactory,
            NumberPropertyReconciler reconciler,
            @Qualifier("numberPropertyInformer") SharedIndexInformer<KnxNumberProperty> numberPropertyInformer
    ) {
        SystemInstanceFilter filter = new SystemInstanceFilter();
        return ControllerBuilder
                .defaultBuilder(informerFactory)
                .watch(workQueue -> ControllerBuilder
                        .controllerWatchBuilder(KnxNumberProperty.class, workQueue)
                        .withResyncPeriod(Duration.of(1, ChronoUnit.SECONDS))
                        .withOnAddFilter(filter::test)
                        .withOnUpdateFilter((before, after) -> filter.test(after))
                        .withOnDeleteFilter((res, flag) -> filter.test(res))
                        .build())
                .withWorkerCount(5)
                .withReconciler(reconciler)
                .withReadyFunc(numberPropertyInformer::hasSynced)
                .withName("NumberPropertyController")
                .build();
    }


    private class SystemInstanceFilter implements Predicate<Property> {
        @Override
        public boolean test(Property property) {
            Optional<PropertySystemAddress> address = Optional.ofNullable(property)
                    .map(Property::getSpec)
                    .map(PropertySpec::getAddress);

            if (address.isPresent()) {
                PropertySystemAddress addrVal = address.get();
                return SYSTEM_TYPE.equals(addrVal.getSystem());
            }
            return false;
        }
    }

}
