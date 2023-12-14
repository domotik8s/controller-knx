package io.domotik8s.knxcontroller.k8s.reconciler;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import org.springframework.stereotype.Component;

@Component
public class StringPropertyReconciler implements Reconciler {

    @Override
    public Result reconcile(Request request) {
        System.out.println(request);
        return new Result(false);
    }

}
