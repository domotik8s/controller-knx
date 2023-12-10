package io.domotik8s.knxcontroller.k8s.ctrl.light;

import io.domotik8s.knxcontroller.k8s.ctrl.model.AddressPair;
import io.domotik8s.knxcontroller.k8s.ctrl.light.model.V1Beta1KnxLight;
import io.domotik8s.knxcontroller.k8s.ctrl.light.model.V1Beta1KnxLightList;
import io.domotik8s.knxcontroller.k8s.ctrl.light.model.V1Beta1LightKnxConnectionConfig;
import io.domotik8s.knxcontroller.knx.client.GroupAddressListener;
import io.domotik8s.knxcontroller.knx.client.KnxClient;
import io.domotik8s.knxcontroller.knx.convert.StringToDptConverter;
import io.domotik8s.knxcontroller.knx.convert.StringToGroupAddressConverter;
import io.domotik8s.model.*;
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
public class LightSyncer implements ResourceEventHandler<V1Beta1KnxLight>, GroupAddressListener {

    private final Logger logger = LoggerFactory.getLogger(LightSyncer.class);


    private final SharedIndexInformer<V1Beta1KnxLight> informer;

    private final GenericKubernetesApi<V1Beta1KnxLight, V1Beta1KnxLightList> lightClient;

    private final KnxClient knxClient;

    private final Set<GroupAddress> subscriptions = new HashSet<>();


    @PostConstruct
    public void setup() {
        knxClient.addGroupAddressListener(this);
        informer.addEventHandler(this);
    }


    /*
     * GroupAddressListener (Calls from KNX)
     */

    @Override
    public void groupWrite(IndividualAddress source, GroupAddress destination, byte[] asdu) {
        logger.debug("Received group write on KNX bus to address {} with asdu {}", destination, asdu);
        updateLightState(destination, asdu);
    }

    @Override
    public void groupReadResponse(IndividualAddress source, GroupAddress destination, byte[] asdu) {
        logger.debug("Received read response on KNX bus to address {} with asdu {}", destination, asdu);
        updateLightState(destination, asdu);
    }

    @Override
    public boolean subscribesTo(GroupAddress address) {
        return subscriptions.contains(address);
    }


    /*
     * ResourceEventHandler (Calls from K8s)
     */

    @Override
    public void onAdd(V1Beta1KnxLight v1beta1Light) {
        subscribe(v1beta1Light);
    }

    @Override
    public void onUpdate(V1Beta1KnxLight v1beta1Light, V1Beta1KnxLight apiType1) {
        subscribe(v1beta1Light);
    }

    @Override
    public void onDelete(V1Beta1KnxLight v1beta1Light, boolean b) {
        unsubscribe(v1beta1Light);
    }


    /*
     * Syncer Methods
     */

    private void updateLightState(GroupAddress destination, byte[] asdu) {
        // Get all available lights
        KubernetesApiResponse<V1Beta1KnxLightList> listResp = lightClient.list();
        V1Beta1KnxLightList list = listResp.getObject();

        // Find the light that has destination as a read address and update that state property
        for (V1Beta1KnxLight v1beta1Light: list.getItems()) {
            Optional<V1Beta1LightKnxConnectionConfig> config = Optional.ofNullable(v1beta1Light.getSpec())
                    .map(V1beta1LightSpec::getConnection)
                    .map(V1beta1LightSpecConnection::getConfig);

            // Check power
            Optional<String> powerGAStr = config.map(V1Beta1LightKnxConnectionConfig::getPower)
                    .map(AddressPair::getRead);

            Optional<String> powerDPTStr = config.map(V1Beta1LightKnxConnectionConfig::getPower)
                    .map(AddressPair::getDpt);

            StringToGroupAddressConverter converter = new StringToGroupAddressConverter();
            GroupAddress powerGA = converter.convert(powerGAStr.get());

            StringToDptConverter dptConverter = new StringToDptConverter();
            DPT powerDPT = dptConverter.convert(powerDPTStr.get());

            if (destination.equals(powerGA)) {
                // Update
                DPTXlator xlator = null;
                try {
                    xlator = TranslatorTypes.createTranslator(powerDPT, asdu);
                } catch (KNXException e) {
                    throw new RuntimeException(e);
                }
                DPTXlatorBoolean boolXlator = (DPTXlatorBoolean) xlator;
                Boolean value = boolXlator.getValueBoolean();

                V1beta1LightStatus status = Optional.ofNullable(v1beta1Light.getStatus()).orElse(new V1beta1LightStatus());

                V1beta1LightState state = Optional.ofNullable(status.getState()).orElse(new V1beta1LightState());
                status.setState(state);

                state.setPower(value);

                v1beta1Light.setStatus(status);

                if (!Boolean.TRUE.equals(v1beta1Light.getSpec().getEnforce())) {
                    v1beta1Light.getSpec().setState(state);
                    lightClient.update(v1beta1Light);
                }

                lightClient.updateStatus(v1beta1Light, (l) -> l.getStatus());

                return;
            }
        }
    }

    private void subscribe(V1Beta1KnxLight v1beta1Light) {
        Optional<V1Beta1LightKnxConnectionConfig> config = Optional.ofNullable(v1beta1Light.getSpec())
                .map(V1beta1LightSpec::getConnection)
                .map(V1beta1LightSpecConnection::getConfig);

        if (config.isEmpty()) return;

        // Power
        Optional<String> powerGAStr = config.map(V1Beta1LightKnxConnectionConfig::getPower)
                .map(AddressPair::getRead);

        if (powerGAStr.isPresent()) {
            StringToGroupAddressConverter converter = new StringToGroupAddressConverter();
            GroupAddress powerGA = converter.convert(powerGAStr.get());
            subscriptions.add(powerGA);
            logger.debug("Subscribed to GA {} to receive power updates", powerGA);
        }

        // Brightness

        // Color
    }

    private void unsubscribe(V1Beta1KnxLight v1beta1Light) {
        Optional<V1Beta1LightKnxConnectionConfig> config = Optional.ofNullable(v1beta1Light.getSpec())
                .map(V1beta1LightSpec::getConnection)
                .map(V1beta1LightSpecConnection::getConfig);

        if (config.isEmpty()) return;

        // Power
        Optional<String> powerGAStr = config.map(V1Beta1LightKnxConnectionConfig::getPower)
                .map(AddressPair::getRead);

        if (powerGAStr.isPresent()) {
            StringToGroupAddressConverter converter = new StringToGroupAddressConverter();
            GroupAddress powerGA = converter.convert(powerGAStr.get());
            subscriptions.remove(powerGA);
            logger.debug("Unsubscribed to GA {} to receive power updates", powerGA);
        }

        // Brightness

        // Color
    }

}
