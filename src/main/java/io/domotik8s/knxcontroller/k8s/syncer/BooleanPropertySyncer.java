package io.domotik8s.knxcontroller.k8s.syncer;

import io.domotik8s.knxcontroller.k8s.utils.DptSemanticsConverter;
import io.domotik8s.knxcontroller.k8s.model.*;
import io.domotik8s.knxcontroller.knx.client.KnxClient;
import io.domotik8s.model.bool.BooleanPropertyState;
import io.domotik8s.model.bool.BooleanPropertyStatus;
import io.domotik8s.model.bool.BooleanSemantic;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.dptxlator.DPT;
import tuwien.auto.calimero.dptxlator.DPTXlator;
import tuwien.auto.calimero.dptxlator.DPTXlatorBoolean;
import tuwien.auto.calimero.dptxlator.TranslatorTypes;

import java.util.Optional;

@Component
public class BooleanPropertySyncer extends AbstractSyncer<KnxBooleanProperty> {

    private final GenericKubernetesApi<KnxBooleanProperty, KnxBooleanPropertyList> client;



    public BooleanPropertySyncer(
            @Autowired KnxClient knxClient,
            @Autowired SharedIndexInformer<KnxBooleanProperty> informer,
            @Autowired GenericKubernetesApi<KnxBooleanProperty, KnxBooleanPropertyList> client
    ) {
        super(knxClient, informer);
        this.client = client;
    }


    @Override
    public Optional<String> extractReadAddress(KnxBooleanProperty property) {
        return Optional.ofNullable(property)
                .map(KnxBooleanProperty::getSpec)
                .map(KnxBooleanPropertySpec::getAddress)
                .map(KnxPropertyAddress::getRead);
    }


    @Override
    public void updateCurrentState(GroupAddress destination, byte[] asdu) {
        // Get all available lights
        KnxBooleanPropertyList list = client.list().getObject();

        // Find the light that has destination as a read address and update that state property
        Optional<KnxBooleanProperty> propertyOpt = list.getItems().stream().filter(property -> {
            Optional<KnxPropertyAddress> config = Optional.ofNullable(property.getSpec()).map(KnxBooleanPropertySpec::getAddress);
            if (config.isPresent()) {
                Optional<String> gaStr = config.map(KnxPropertyAddress::getRead);
                if (gaStr.isPresent()) {
                    return destination.equals(getGaConverter().convert(gaStr.get()));
                }
            }
            return false;
        }).findFirst();

        if (propertyOpt.isEmpty()) return;

        // Extract the DPT for value conversion
        KnxBooleanProperty property = propertyOpt.get();;
        Optional<KnxPropertyAddress> config = Optional.ofNullable(property.getSpec()).map(KnxBooleanPropertySpec::getAddress);
        Optional<String> dptStr = config.map(KnxPropertyAddress::getDpt);
        DPT dpt = getDptConverter().convert(dptStr.get());

        if (dpt == null) return;

        // Convert the received value
        DPTXlator xlator = null;
        try {
            xlator = TranslatorTypes.createTranslator(dpt, asdu);
        } catch (KNXException e) {
            throw new RuntimeException(e);
        }
        DPTXlatorBoolean boolXlator = (DPTXlatorBoolean) xlator;
        Boolean value = boolXlator.getValueBoolean();


        boolean semanticsUpdated = updateSemantics(property, dpt);
        boolean desiredStateUpdated = updateDesiredState(property, value);

        if (semanticsUpdated || desiredStateUpdated)
            client.update(property);

        // Update the resource's current state
        if (config.get().getRead() != null) {
            BooleanPropertyStatus status = Optional.ofNullable(property.getStatus()).orElse(new BooleanPropertyStatus());
            property.setStatus(status);

            BooleanPropertyState state = Optional.ofNullable(status.getState()).orElse(new BooleanPropertyState());
            status.setState(state);

            state.setValue(value);

            client.updateStatus(property, (l) -> l.getStatus());
        } else if(property.getStatus().getState() != null) {
            property.getStatus().setState(null);
            client.updateStatus(property, (l) -> l.getStatus());
        }
    }

    private boolean updateDesiredState(KnxBooleanProperty property, Boolean value) {
        Optional<KnxPropertyAddress> config = Optional.ofNullable(property.getSpec()).map(KnxBooleanPropertySpec::getAddress);
        // Update the resource's desired state
        if (config.get().getWrite() != null) {
            getLogger().debug("Resource {} as a write address, which means we can have a desired state.", property.getMetadata().getName());
            KnxBooleanPropertySpec spec = Optional.ofNullable(property.getSpec()).orElse(new KnxBooleanPropertySpec());
            property.setSpec(spec);

            if (!Boolean.TRUE.equals(spec.getLocked())) {
                BooleanPropertyState dState = Optional.ofNullable(spec.getState()).orElse(new BooleanPropertyState());
                spec.setState(dState);

                dState.setValue(value);
                return true;
            }
        } else if(property.getSpec().getState() != null) {
            getLogger().debug("Resource {} as NO write address but a desired state is set. Deleting..", property.getMetadata().getName());
            property.getSpec().setState(null);
            return true;
        }
        return false;
    }

    private boolean updateSemantics(KnxBooleanProperty property, DPT dpt) {
        Optional<KnxBooleanPropertySpec> specOpt = Optional.ofNullable(property).map(KnxBooleanProperty::getSpec);
        if (specOpt.isPresent()) {
            KnxBooleanPropertySpec spec = specOpt.get();
            BooleanSemantic semantic = Optional.ofNullable(property.getSpec().getSemantic()).orElse(new BooleanSemantic());
            spec.setSemantic(semantic);
            if (semantic.getMeaning() == null) {
                spec.setSemantic(DptSemanticsConverter.dptToSemantic(dpt));
                return true;
            }
        }
        return false;
    }


}
