package io.domotik8s.knxcontroller.k8s;

import io.domotik8s.knxcontroller.k8s.ctrl.light.model.V1Beta1KnxLight;
import io.domotik8s.knxcontroller.k8s.ctrl.light.model.V1Beta1KnxLightList;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
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

import static io.domotik8s.knxcontroller.k8s.Constants.*;

@Configuration
public class K8sConfig {

    private Logger logger = LoggerFactory.getLogger(K8sConfig.class);


    @Bean
    public ApiClient apiClient() throws IOException {
        ApiClient client = Config.defaultClient();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
        return client;
    }

    @Bean(name = "lightClient")
    public GenericKubernetesApi<V1Beta1KnxLight, V1Beta1KnxLightList> lightClient(ApiClient client) {
        return new GenericKubernetesApi(
                V1Beta1KnxLight.class, V1Beta1KnxLightList.class,
                API_GROUP, API_VERSION, LIGHT_PLURAL,
                client
        );
    }

    @Bean
    public SharedIndexInformer<V1Beta1KnxLight> lightInformer(SharedInformerFactory informerFactory, GenericKubernetesApi<V1Beta1KnxLight, V1Beta1KnxLightList> lightClient) {
        return informerFactory.sharedIndexInformerFor(lightClient, V1Beta1KnxLight.class, 0);
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
