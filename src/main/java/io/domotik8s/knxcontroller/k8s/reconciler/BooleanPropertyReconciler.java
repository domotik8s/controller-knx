package io.domotik8s.knxcontroller.k8s.reconciler;

import io.domotik8s.knxcontroller.k8s.utils.DptSemanticsConverter;
import io.domotik8s.model.PropertySpec;
import io.domotik8s.knxcontroller.k8s.model.KnxBooleanProperty;
import io.domotik8s.knxcontroller.k8s.model.KnxBooleanPropertyList;
import io.domotik8s.knxcontroller.k8s.model.KnxBooleanPropertySpec;
import io.domotik8s.knxcontroller.k8s.model.KnxPropertyAddress;
import io.domotik8s.knxcontroller.knx.client.KnxClient;
import io.domotik8s.knxcontroller.knx.convert.StringToDptConverter;
import io.domotik8s.knxcontroller.knx.convert.StringToGroupAddressConverter;
import io.domotik8s.model.PropertyAccess;
import io.domotik8s.model.bool.BooleanPropertyState;
import io.domotik8s.model.bool.BooleanPropertyStatus;
import io.domotik8s.model.bool.BooleanSemantic;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.dptxlator.DPT;
import tuwien.auto.calimero.dptxlator.DPTXlatorBoolean;
import tuwien.auto.calimero.dptxlator.TranslatorTypes;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BooleanPropertyReconciler implements Reconciler {

    private Logger logger = LoggerFactory.getLogger(BooleanPropertyReconciler.class);

    @Qualifier("booleanPropertyClient")
    private final GenericKubernetesApi<KnxBooleanProperty, KnxBooleanPropertyList> client;

    @Qualifier("booleanPropertyInformer")
    private final SharedIndexInformer<KnxBooleanProperty> informer;

    private final KnxClient knxClient;

    private final StringToGroupAddressConverter gaConverter = new StringToGroupAddressConverter();

    private final StringToDptConverter dptConverter = new StringToDptConverter();


    @Override
    public Result reconcile(Request request) {
        String key = createKey(request);
        logger.trace("Handling resource {}", key);

        KnxBooleanProperty resource = informer.getIndexer().getByKey(key);

        boolean accessUpdated = updateAccess(resource);
        boolean semanticsUpdated = updateSemantics(resource);

        if (accessUpdated || semanticsUpdated) {
            client.update(resource);
        }

        // Get desired and current state
        Optional<Boolean> desiredOpt = Optional.ofNullable(resource)
                .map(KnxBooleanProperty::getSpec)
                .map(KnxBooleanPropertySpec::getState)
                .map(BooleanPropertyState::getValue);

        Optional<Boolean> currentOpt = Optional.ofNullable(resource)
                .map(KnxBooleanProperty::getStatus)
                .map(BooleanPropertyStatus::getState)
                .map(BooleanPropertyState::getValue);

        logger.debug("Resource {} has current state {} and desired state {}", resource.getMetadata().getName(), currentOpt.orElse(null), desiredOpt.orElse(null));

        // If desired and current state are null, this means we don't know the state
        // and also don't want to change it. So we request a state update from the system.
        if (desiredOpt.isEmpty() && currentOpt.isEmpty()) {
            requestStateUpdate(resource);

        // If desired state is set but it does not equal the current state, we want
        // to update the system accordingly
        } else if (desiredOpt.isPresent() && !desiredOpt.get().equals(currentOpt.orElse(null))) {
            updateSystemState(resource);
        }

        return new Result(false);
    }

    private boolean updateAccess(KnxBooleanProperty resource) {
        Optional<KnxBooleanPropertySpec> spec = Optional.ofNullable(resource).map(KnxBooleanProperty::getSpec);
        Optional<KnxPropertyAddress> address = spec
                .map(KnxBooleanPropertySpec::getAddress);

        if (address.isEmpty()) return false;

        Set<PropertyAccess> before = Optional.ofNullable(spec.get().getAccess()).orElse(new HashSet<>());

        Set<PropertyAccess> after = new HashSet<>();
        if (address.get().getDpt() != null) {
            if (address.get().getRead() != null)
                after.add(PropertyAccess.READ);
            if (address.get().getWrite() != null)
                after.add(PropertyAccess.WRITE);
        }

        if (before.containsAll(after) && after.containsAll(before)) {
            return false;
        }

        spec.get().setAccess(after);
        return true;
    }


    private void requestStateUpdate(KnxBooleanProperty resource){
            Optional<KnxPropertyAddress> address = extractPropertyAddress(resource);

            if (address.isEmpty()) {
                logger.warn("Resource {} does not have a property address configured.", resource.getMetadata().getName());
                return;
            }

            String gaStr = address.get().getRead();
            String dptStr = address.get().getDpt();

            if (gaStr == null) {
                logger.debug("Resource {} is write only since there is not read address configured", resource.getMetadata().getName());
            } else if (gaStr != null && dptStr != null) {
                GroupAddress ga = gaConverter.convert(gaStr);
                DPT dpt = dptConverter.convert(dptStr);
                logger.debug("Request status update from KNX for GA {}", ga);
                knxClient.read(dpt, ga);
            } else {
                logger.error("Resource {} does not have a DPT configured.", resource.getMetadata().getName());
            }
    }

    private void updateSystemState(KnxBooleanProperty resource) {
        Optional<Boolean> desiredOpt = Optional.ofNullable(resource)
                .map(KnxBooleanProperty::getSpec)
                .map(PropertySpec::getState)
                .map(BooleanPropertyState::getValue);

        if (desiredOpt.isEmpty()) {
            logger.warn("No desired state set for resource {}.", resource.getMetadata().getName());
            return;
        }

        Optional<KnxPropertyAddress> address = extractPropertyAddress(resource);
        Optional<String> gaOpt = address.map(KnxPropertyAddress::getWrite);
        Optional<String> dptOpt = address.map(KnxPropertyAddress::getDpt);

        if (gaOpt.isEmpty() || dptOpt.isEmpty()) {
            logger.warn("Cannot update system for {} with value {}. Either group address or dpt is not set.", resource.getMetadata().getName(), desiredOpt.get());
            return;
        }

        GroupAddress ga = gaConverter.convert(gaOpt.get());
        DPT dpt = dptConverter.convert(dptOpt.get());

        DPTXlatorBoolean xlator = null;
        try {
            xlator = (DPTXlatorBoolean) TranslatorTypes.createTranslator(0, dpt.getID());
        } catch (KNXException e) {
            throw new RuntimeException(e);
        }
        xlator.setValue(desiredOpt.get());

        logger.debug("Sending to GA {}: {}", ga, desiredOpt.get());
        knxClient.write(ga, xlator);
    }

    private Optional<KnxPropertyAddress> extractPropertyAddress(KnxBooleanProperty resource) {
        return Optional.ofNullable(resource)
                .map(KnxBooleanProperty::getSpec)
                .map(KnxBooleanPropertySpec::getAddress);
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

    private boolean updateSemantics(KnxBooleanProperty property) {
        Optional<KnxBooleanPropertySpec> specOpt = Optional.ofNullable(property).map(KnxBooleanProperty::getSpec);
        Optional<String> dptOpt = specOpt.map(KnxBooleanPropertySpec::getAddress).map(KnxPropertyAddress::getDpt);

        if (specOpt.isPresent() && dptOpt.isPresent()) {
            KnxBooleanPropertySpec spec = specOpt.get();
            BooleanSemantic semantic = Optional.ofNullable(property.getSpec().getSemantic()).orElse(new BooleanSemantic());
            spec.setSemantic(semantic);
            if (semantic.getMeaning() == null) {
                spec.setSemantic(DptSemanticsConverter.dptToSemantic(dptOpt.get()));
                return true;
            }
        }
        return false;
    }

}
