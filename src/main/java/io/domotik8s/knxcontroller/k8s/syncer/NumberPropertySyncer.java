package io.domotik8s.knxcontroller.k8s.syncer;

import io.domotik8s.knxcontroller.k8s.model.KnxNumberProperty;
import io.domotik8s.knxcontroller.k8s.model.KnxNumberPropertyList;
import io.domotik8s.knxcontroller.k8s.model.KnxNumberPropertySpec;
import io.domotik8s.knxcontroller.k8s.model.KnxPropertyAddress;
import io.domotik8s.knxcontroller.knx.client.GroupAddressListener;
import io.domotik8s.knxcontroller.knx.client.KnxClient;
import io.domotik8s.knxcontroller.knx.convert.StringToDptConverter;
import io.domotik8s.knxcontroller.knx.convert.StringToGroupAddressConverter;
import io.domotik8s.knxcontroller.k8s.utils.DptUnitConverter;
import io.domotik8s.model.PropertyList;
import io.domotik8s.model.num.NumberPropertySpec;
import io.domotik8s.model.num.NumberPropertyState;
import io.domotik8s.model.num.NumberPropertyStatus;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.dptxlator.DPT;
import tuwien.auto.calimero.dptxlator.DPTXlator;
import tuwien.auto.calimero.dptxlator.TranslatorTypes;

import javax.annotation.PostConstruct;
import javax.measure.Unit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NumberPropertySyncer implements ResourceEventHandler<KnxNumberProperty>, GroupAddressListener {

    private Logger logger = LoggerFactory.getLogger(NumberPropertySyncer.class);


    private final KnxClient knxClient;

    private final GenericKubernetesApi<KnxNumberProperty, KnxNumberPropertyList> client;

    private final SharedIndexInformer<KnxNumberProperty> informer;


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
    public void onAdd(KnxNumberProperty property) {
        subscribe(property);
    }

    @Override
    public void onUpdate(KnxNumberProperty property, KnxNumberProperty apiType1) {
        subscribe(property);
    }

    @Override
    public void onDelete(KnxNumberProperty property, boolean b) {
        unsubscribe(property);
    }


    /*
     * Syncer Methods
     */

    /**
     * Takes group address and data incoming from KNX bus, finds the matching resource and updates
     * the current AND desired state accordingly.
     * @param destination The group address that reported a new value
     * @param asdu The data carried by the KNX message
     */
    private void updateCurrentState(GroupAddress destination, byte[] asdu) {
        // Get all available lights
        KubernetesApiResponse<KnxNumberPropertyList> listResp = client.list();
        PropertyList<KnxNumberProperty> list = listResp.getObject();

        // Find the light that has destination as a read address and update that state property
        Optional<KnxNumberProperty> propertyOpt = list.getItems().stream().filter(property -> {
            Optional<KnxPropertyAddress> config = Optional.ofNullable(property.getSpec()).map(NumberPropertySpec::getAddress);
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
        KnxNumberProperty property = propertyOpt.get();;
        Optional<KnxPropertyAddress> config = Optional.ofNullable(property.getSpec()).map(NumberPropertySpec::getAddress);
        Optional<String> dptStr = config.map(KnxPropertyAddress::getDpt);
        DPT dpt = dptConverter.convert(dptStr.get());

        if (dpt == null) return;

        // Convert the received value
        DPTXlator xlator = null;
        Double dblValue = null;
        try {
            xlator = TranslatorTypes.createTranslator(dpt, asdu);
            dblValue = xlator.getNumericValue();
        } catch (KNXException e) {
            throw new RuntimeException(e);
        }


        // Update the resource's desired state
        if (config.get().getWrite() != null) {
            logger.debug("Resource {} as a write address, which means we can have a desired state.", property.getMetadata().getName());
            KnxNumberPropertySpec spec = Optional.ofNullable(property.getSpec()).orElse(new KnxNumberPropertySpec());
            property.setSpec(spec);

            if (!Boolean.TRUE.equals(spec.getLocked())) {
                NumberPropertyState dState = Optional.ofNullable(spec.getState()).orElse(new NumberPropertyState());
                dState.setUnit(Optional.ofNullable(DptUnitConverter.toUnit(dpt)).map(Unit::toString).orElse(null));
                try {
                    dState.setValue(toNumber(xlator, dpt));
                } catch (KNXFormatException e) {
                    throw new RuntimeException(e);
                }
                spec.setState(dState);

                client.update(property);
            }
        } else if(property.getSpec().getState() != null) {
            logger.debug("Resource {} as NO write address but a desired state is set. Deleting..", property.getMetadata().getName());
            property.getSpec().setState(null);
            client.update(property);
        }

        // Update the resource's current state
        if (config.get().getRead() != null) {
            NumberPropertyStatus status = Optional.ofNullable(property.getStatus()).orElse(new NumberPropertyStatus());
            property.setStatus(status);

            NumberPropertyState state = Optional.ofNullable(status.getState()).orElse(new NumberPropertyState());
            state.setUnit(Optional.ofNullable(DptUnitConverter.toUnit(dpt)).map(Unit::toString).orElse(null));
            try {
                state.setValue(toNumber(xlator, dpt));
            } catch (KNXFormatException e) {
                throw new RuntimeException(e);
            }
            status.setState(state);

            client.updateStatus(property, (l) -> l.getStatus());
        } else if(property.getStatus().getState() != null) {
            property.getStatus().setState(null);
            client.updateStatus(property, (l) -> l.getStatus());
        }
    }

    private Number toNumber(DPTXlator xlator, DPT dpt) throws KNXFormatException {
        String dptId = dpt.getID();
        String[] dptIdTokens = dptId.split("\\.");
        Integer dptValue = Integer.parseInt(dptIdTokens[0]);

        Number value = Double.valueOf(xlator.getNumericValue());
        if ((dptValue >= 2 && dptValue <= 8) ||
                (dptValue >= 10 && dptValue <= 13) ||
                (dptValue >= 20 && dptValue <= 211) ||
                dptValue == 214 || dptValue == 217 ||
                dptValue == 220 ||
                dptValue == 223 ||
                dptValue == 225 ||
                (dptValue >= 231 && dptValue <= 234) ||
                (dptValue >= 236 && dptValue <= 241)
        ) {
            value = value.longValue();
        }
        return value;
    }


    /**
     * Adds the given resource's read address to the list of subscribed group addresses
     * @param property The property to start listening for changes
     */
    private void subscribe(KnxNumberProperty property) {
        Optional<KnxPropertyAddress> config = Optional.ofNullable(property.getSpec())
                .map(KnxNumberPropertySpec::getAddress);
        if (config.isPresent()) {
            config.map(KnxPropertyAddress::getRead).ifPresent((value) -> {
                GroupAddress ga = gaConverter.convert(value);
                subscriptions.add(ga);
                logger.debug("Subscribed to GA {} to receive power updates", value);
            });
        }
    }

    /**
     * Remove the given resource's read address to the list of subscribed group addresses
     * @param property The property to stop listening for changes
     */
    private void unsubscribe(KnxNumberProperty property) {
        Optional<KnxPropertyAddress> config = Optional.ofNullable(property.getSpec())
                .map(KnxNumberPropertySpec::getAddress);
        if (config.isPresent()) {
            config.map(KnxPropertyAddress::getRead).ifPresent((value) -> {
                GroupAddress ga = gaConverter.convert(value);
                subscriptions.remove(ga);
                logger.debug("Unsubscribed to GA {} to receive updates", value);
            });
        }
    }

}
