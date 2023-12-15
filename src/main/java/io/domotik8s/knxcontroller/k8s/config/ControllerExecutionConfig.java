package io.domotik8s.knxcontroller.k8s.config;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.informer.SharedInformerFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ControllerExecutionConfig implements ApplicationRunner {

    private Logger logger = LoggerFactory.getLogger(ControllerExecutionConfig.class);

    private final SharedInformerFactory informerFactory;

    private final List<Controller> controllers;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        ExecutorService executorService = Executors.newCachedThreadPool();
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
    }

}
