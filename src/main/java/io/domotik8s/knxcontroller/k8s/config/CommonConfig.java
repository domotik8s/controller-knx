package io.domotik8s.knxcontroller.k8s.config;

import io.domotik8s.model.generic.Property;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Configuration
public class CommonConfig {

    private Logger logger = LoggerFactory.getLogger(CommonConfig.class);

    @Bean
    public ApiClient apiClient() throws IOException {
        ApiClient client = io.kubernetes.client.util.Config.defaultClient();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
        return client;
    }

    @Bean
    public ExecutorService executorService() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public ApplicationRunner runner(ExecutorService executorService, SharedInformerFactory informerFactory, List<Controller> controllers) {
        return args -> {
            // Register Shutdown Hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.debug("Shutdown signal received. Stopping controllers and informers.");

                informerFactory.stopAllRegisteredInformers();
                controllers.forEach(Controller::shutdown);
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                }

                logger.debug("All resources have been gracefully shut down.");
            }));

            // Start informers and controllers
            executorService.execute(() -> {
                informerFactory.startAllRegisteredInformers();
                controllers.forEach(Controller::run);
            });
        };
    }

}
