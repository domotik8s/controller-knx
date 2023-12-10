package io.domotik8s.knxcontroller.k8s.ctrl;

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractReconciler<T extends KubernetesObject, L extends KubernetesListObject> implements Reconciler {

    private Logger logger = LoggerFactory.getLogger(getClass());


    public abstract SharedIndexInformer<T> getInformer();

    public abstract GenericKubernetesApi<T, L> getClient();

    protected Logger getLogger() {
        return logger;
    }

    protected String createKey(Request request) {
        StringBuilder key = new StringBuilder();
        if (request.getNamespace() != null) {
            key.append(request.getNamespace());
            key.append("/");
        }
        key.append(request.getName());
        return key.toString();
    }

    protected boolean shouldUpdate(Object desiredValue, Object currentValue) {
        return desiredValue != null && !desiredValue.equals(currentValue);
    }

    @Override
    public Result reconcile(Request request) {
        String key = createKey(request);
        T resource = getInformer().getIndexer().getByKey(key);

        if (resource == null) {
            getLogger().info("Received reconciliation request for {} but resource is unknown", key);
            return new Result(false);
        }

        getLogger().info("Reconciling {}", key);

        try {
            reconcileState(resource);
        } catch (Throwable e) {
            getLogger().error("Error during reconciliation", e);
            return new Result(true);
        }

        return new Result(false);
    }

    protected abstract void reconcileState(T resource) throws Exception;

}
