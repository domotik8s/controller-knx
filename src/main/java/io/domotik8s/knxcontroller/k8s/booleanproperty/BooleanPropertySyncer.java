package io.domotik8s.knxcontroller.k8s.booleanproperty;

import io.domotik8s.knxcontroller.k8s.model.KnxBooleanPropertyList;
import io.domotik8s.knxcontroller.k8s.model.KnxBooleanPropertySpec;
import io.domotik8s.knxcontroller.k8s.model.KnxPropertyAddress;
import io.domotik8s.knxcontroller.knx.client.GroupAddressListener;
import io.domotik8s.knxcontroller.knx.client.KnxClient;
import io.domotik8s.knxcontroller.knx.convert.StringToDptConverter;
import io.domotik8s.knxcontroller.knx.convert.StringToGroupAddressConverter;
import io.domotik8s.knxcontroller.k8s.model.KnxBooleanProperty;
import io.domotik8s.model.bool.BooleanPropertyState;
import io.domotik8s.model.bool.BooleanPropertyStatus;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.dptxlator.DPT;
import tuwien.auto.calimero.dptxlator.DPTXlator;
import tuwien.auto.calimero.dptxlator.DPTXlatorBoolean;
import tuwien.auto.calimero.dptxlator.TranslatorTypes;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BooleanPropertySyncer implements ResourceEventHandler<KnxBooleanProperty>, GroupAddressListener {

    private Logger logger = LoggerFactory.getLogger(BooleanPropertySyncer.class);


    private final KnxClient knxClient;

    private final GenericKubernetesApi<KnxBooleanProperty, KnxBooleanPropertyList> client;

    private final SharedIndexInformer<KnxBooleanProperty> informer;


    private final Set<GroupAddress> subscriptions = new HashSet<>();

    private final StringToGroupAddressConverter gaConverter = new StringToGroupAddressConverter();

    private final StringToDptConverter dptConverter = new StringToDptConverter();


    @PostConstruct
    private void register() {
        knxClient.addGroupAddressListener(this);
        informer.addEventHandler(this);
    }


    /*
     * GroupAddressListener
     */

    public void groupWrite(IndividualAddress source, GroupAddress destination, byte[] asdu) {
        updateCurrentState(destination, asdu);
    }

    @Override
    public void groupReadResponse(IndividualAddress source, GroupAddress destination, byte[] asdu) {
        updateCurrentState(destination, asdu);
    }

    @Override
    public boolean subscribesTo(GroupAddress address) {
        return subscriptions.contains(address);
    }


    /*
     * ResourceEventHandler
     */

    @Override
    public void onAdd(KnxBooleanProperty property) {
        subscribe(property);
    }

    @Override
    public void onUpdate(KnxBooleanProperty property, KnxBooleanProperty apiType1) {
        subscribe(property);
    }

    @Override
    public void onDelete(KnxBooleanProperty property, boolean b) {
        unsubscribe(property);
    }


    /*
     * Syncer Methods
     */

    private void updateCurrentState(GroupAddress destination, byte[] asdu) {
        // Get all available lights
        KnxBooleanPropertyList list = client.list().getObject();

        // Find the light that has destination as a read address and update that state property
        Optional<KnxBooleanProperty> propertyOpt = list.getItems().stream().filter(property -> {
            Optional<KnxPropertyAddress> config = Optional.ofNullable(property.getSpec()).map(KnxBooleanPropertySpec::getAddress);
            if (config.isPresent()) {
                Optional<String> gaStr = config.map(KnxPropertyAddress::getRead);
                if (gaStr.isPresent()) {
                    return destination.equals(gaConverter.convert(gaStr.get()));
                }
            }
            return false;
        }).findFirst();

        if (propertyOpt.isEmpty()) return;

        // Extract the DPT for value conversion
        KnxBooleanProperty property = propertyOpt.get();;
        Optional<KnxPropertyAddress> config = Optional.ofNullable(property.getSpec()).map(KnxBooleanPropertySpec::getAddress);
        Optional<String> dptStr = config.map(KnxPropertyAddress::getDpt);
        DPT dpt = dptConverter.convert(dptStr.get());

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


        // Update the resource's desired state
        KnxBooleanPropertySpec spec = Optional.ofNullable(property.getSpec()).orElse(new KnxBooleanPropertySpec());
        property.setSpec(spec);

        BooleanPropertyState dState = Optional.ofNullable(spec.getState()).orElse(new BooleanPropertyState());
        dState.setValue(value);
        spec.setState(dState);

        client.update(property);

        // Update the resource's current state
        BooleanPropertyStatus status = Optional.ofNullable(property.getStatus()).orElse(new BooleanPropertyStatus());
        property.setStatus(status);

        BooleanPropertyState state = Optional.ofNullable(status.getState()).orElse(new BooleanPropertyState());
        state.setValue(value);
        status.setState(state);

        client.updateStatus(property, (l) -> l.getStatus());
    }

    private void subscribe(KnxBooleanProperty property) {
        Optional<KnxPropertyAddress> config = Optional.ofNullable(property.getSpec())
                .map(KnxBooleanPropertySpec::getAddress);
        if (config.isPresent()) {
            config.map(KnxPropertyAddress::getRead).ifPresent((value) -> {
                GroupAddress ga = gaConverter.convert(value);
                subscriptions.add(ga);
                logger.debug("Subscribed to GA {} to receive power updates", value);
            });
        }
    }

    private void unsubscribe(KnxBooleanProperty property) {
        Optional<KnxPropertyAddress> config = Optional.ofNullable(property.getSpec())
                .map(KnxBooleanPropertySpec::getAddress);
        if (config.isPresent()) {
            config.map(KnxPropertyAddress::getRead).ifPresent((value) -> {
                GroupAddress ga = gaConverter.convert(value);
                subscriptions.remove(ga);
                logger.debug("Unsubscribed to GA {} to receive updates", value);
            });
        }
    }

}
