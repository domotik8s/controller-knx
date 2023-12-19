package io.domotik8s.knxcontroller.k8s.syncer;

import io.domotik8s.knxcontroller.k8s.model.*;
import io.domotik8s.knxcontroller.knx.client.KnxClient;
import io.domotik8s.model.bool.BooleanProperty;
import io.domotik8s.model.bool.BooleanPropertyState;
import io.domotik8s.model.bool.BooleanPropertyStatus;
import io.domotik8s.model.bool.BooleanSemantic;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.dptxlator.DPT;
import tuwien.auto.calimero.dptxlator.DPTXlator;
import tuwien.auto.calimero.dptxlator.DPTXlatorBoolean;
import tuwien.auto.calimero.dptxlator.TranslatorTypes;

import java.util.HashMap;
import java.util.Map;
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
                spec.setSemantic(dptToSemantic(dpt));
                return true;
            }
        }
        return false;
    }



    private static Map<String, BooleanSemantic> dptSemanticMap = new HashMap<>();

    static {
        dptSemanticMap.put("1.001", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.OFF_ON).reversed(false).build());
        dptSemanticMap.put("1.002", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.FALSE_TRUE).reversed(false).build());
        dptSemanticMap.put("1.003", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.DISABLE_ENABLE).reversed(false).build());
        dptSemanticMap.put("1.004", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.NORAMP_RAMP).reversed(false).build());
        dptSemanticMap.put("1.005", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.NOALARM_ALARM).reversed(false).build());
        dptSemanticMap.put("1.006", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.LOW_HIGH).reversed(false).build());
        dptSemanticMap.put("1.007", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.DECREASE_INCREASE).reversed(false).build());
        dptSemanticMap.put("1.008", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.UP_DOWN).reversed(false).build());
        dptSemanticMap.put("1.009", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.OPEN_CLOSE).reversed(false).build());
        dptSemanticMap.put("1.010", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.STOP_START).reversed(false).build());
        dptSemanticMap.put("1.011", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.INACTIVE_ACTIVE).reversed(false).build());
        dptSemanticMap.put("1.012", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.NOTINVERTED_INVERTED).reversed(false).build());
        dptSemanticMap.put("1.013", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.STARSTOP_CYCLICALLY).reversed(false).build());
        dptSemanticMap.put("1.014", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.FIXED_CALCULATED).reversed(false).build());
        dptSemanticMap.put("1.015", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.DUMMY_TRIGGER).reversed(false).build());
        dptSemanticMap.put("1.016", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.TRIGGER_TRIGGER).reversed(false).build());
        dptSemanticMap.put("1.017", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.NOTOCCUPIED_OCCUPIED).reversed(false).build());
        dptSemanticMap.put("1.018", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.CLOSED_OPEN).reversed(false).build());
        dptSemanticMap.put("1.019", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.OR_AND).reversed(false).build());
        dptSemanticMap.put("1.021", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.SCENEA_SCENEB).reversed(false).build());
        dptSemanticMap.put("1.022", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.UPDOWN_UPDOWNSTEPSTOP).reversed(false).build());
        dptSemanticMap.put("1.023", BooleanSemantic.builder().meaning(BooleanSemantic.Meaning.COOLING_HEATING).reversed(false).build());

    }

    private BooleanSemantic dptToSemantic(DPT dpt) {
        String dptId = dpt.getID();
        return dptSemanticMap.get(dptId);
    }


}
