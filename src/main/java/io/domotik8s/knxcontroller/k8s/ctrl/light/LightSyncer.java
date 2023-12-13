package io.domotik8s.knxcontroller.k8s.ctrl.light;

import io.domotik8s.knxcontroller.k8s.ctrl.model.PropertyKnxConfig;
import io.domotik8s.knxcontroller.k8s.ctrl.light.model.KnxLight;
import io.domotik8s.knxcontroller.k8s.ctrl.light.model.KnxLightList;
import io.domotik8s.knxcontroller.k8s.ctrl.light.model.KnxLightConnectionConfig;
import io.domotik8s.knxcontroller.knx.client.GroupAddressListener;
import io.domotik8s.knxcontroller.knx.client.KnxClient;
import io.domotik8s.knxcontroller.knx.convert.StringToDptConverter;
import io.domotik8s.knxcontroller.knx.convert.StringToGroupAddressConverter;
import io.domotik8s.model.light.LightSpec;
import io.domotik8s.model.light.LightSpecConnection;
import io.domotik8s.model.light.LightState;
import io.domotik8s.model.light.LightStatus;
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
import tuwien.auto.calimero.dptxlator.*;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LightSyncer implements ResourceEventHandler<KnxLight>, GroupAddressListener {

    private final Logger logger = LoggerFactory.getLogger(LightSyncer.class);


    private final SharedIndexInformer<KnxLight> informer;

    private final GenericKubernetesApi<KnxLight, KnxLightList> lightClient;

    private final KnxClient knxClient;

    private final Set<GroupAddress> subscriptions = new HashSet<>();

    private final StringToGroupAddressConverter gaConverter = new StringToGroupAddressConverter();

    private final StringToDptConverter dptConverter = new StringToDptConverter();

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
    public void onAdd(KnxLight v1beta1Light) {
        subscribe(v1beta1Light);
    }

    @Override
    public void onUpdate(KnxLight v1beta1Light, KnxLight apiType1) {
        subscribe(v1beta1Light);
    }

    @Override
    public void onDelete(KnxLight v1beta1Light, boolean b) {
        unsubscribe(v1beta1Light);
    }


    /*
     * Syncer Methods
     */

    private void updateLightState(GroupAddress destination, byte[] asdu) {
        // Get all available lights
        KubernetesApiResponse<KnxLightList> listResp = lightClient.list();
        KnxLightList list = listResp.getObject();

        // Find the light that has destination as a read address and update that state property
        for (KnxLight v1beta1Light: list.getItems()) {
            Optional<KnxLightConnectionConfig> config = Optional.ofNullable(v1beta1Light.getSpec())
                    .map(LightSpec::getConnection)
                    .map(LightSpecConnection::getConfig);

            if (config.isEmpty()) continue;

            // Check power
            if (config.get().getPower() != null) {
                Optional<String> powerGAStr = config.map(KnxLightConnectionConfig::getPower).map(PropertyKnxConfig::getRead);
                Optional<String> powerDPTStr = config.map(KnxLightConnectionConfig::getPower).map(PropertyKnxConfig::getDpt);
                GroupAddress powerGA = gaConverter.convert(powerGAStr.get());
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

                    LightStatus status = Optional.ofNullable(v1beta1Light.getStatus()).orElse(new LightStatus());

                    LightState state = Optional.ofNullable(status.getState()).orElse(new LightState());
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


            // Check brightness
            if (config.get().getBrightness() != null) {
                Optional<String> brightnessGAStr = config.map(KnxLightConnectionConfig::getBrightness).map(PropertyKnxConfig::getRead);
                Optional<String> brightnessDPTStr = config.map(KnxLightConnectionConfig::getBrightness).map(PropertyKnxConfig::getDpt);
                GroupAddress brightnessGA = gaConverter.convert(brightnessGAStr.get());
                DPT brightnessDPT = dptConverter.convert(brightnessDPTStr.get());

                if (destination.equals(brightnessGA)) {

                    // Update
                    DPTXlator xlator = null;
                    try {
                        xlator = TranslatorTypes.createTranslator(brightnessDPT, asdu);
                    } catch (KNXException e) {
                        throw new RuntimeException(e);
                    }
                    DPTXlator8BitUnsigned floatXlator = (DPTXlator8BitUnsigned) xlator;
                    double value = floatXlator.getNumericValue();

                    LightStatus status = Optional.ofNullable(v1beta1Light.getStatus()).orElse(new LightStatus());

                    LightState state = Optional.ofNullable(status.getState()).orElse(new LightState());
                    status.setState(state);

                    state.setBrightness(value);

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
    }

    private void subscribe(KnxLight v1beta1Light) {
        Optional<KnxLightConnectionConfig> config = Optional.ofNullable(v1beta1Light.getSpec())
                .map(LightSpec::getConnection)
                .map(LightSpecConnection::getConfig);

        if (config.isEmpty()) {
            logger.warn("Unable to subscribe to light {}. Not value found for spec.connection.config", v1beta1Light.getMetadata().getName());
            return;
        }

        // Power
        config.map(KnxLightConnectionConfig::getPower)
                .map(PropertyKnxConfig::getRead)
                .ifPresent(this::subscribe);

        // Brightness
        config.map(KnxLightConnectionConfig::getBrightness)
                .map(PropertyKnxConfig::getRead)
                .ifPresent(this::subscribe);

        // Color
        config.map(KnxLightConnectionConfig::getColor)
                .map(PropertyKnxConfig::getRead)
                .ifPresent(this::subscribe);
    }

    private void subscribe(String gaStr) {
        GroupAddress ga = gaConverter.convert(gaStr);
        subscriptions.add(ga);
        logger.debug("Subscribed to GA {} to receive power updates", gaStr);
    }


    private void unsubscribe(KnxLight v1beta1Light) {
        Optional<KnxLightConnectionConfig> config = Optional.ofNullable(v1beta1Light.getSpec())
                .map(LightSpec::getConnection)
                .map(LightSpecConnection::getConfig);

        if (config.isEmpty()) {
            logger.warn("Unable to unsubscribe from light {}. Not value found for spec.connection.config", v1beta1Light.getMetadata().getName());
            return;
        }

        // Power
        config.map(KnxLightConnectionConfig::getPower)
                .map(PropertyKnxConfig::getRead)
                .ifPresent(this::unsubscribe);

        // Brightness
        config.map(KnxLightConnectionConfig::getBrightness)
                .map(PropertyKnxConfig::getRead)
                .ifPresent(this::unsubscribe);

        // Color
        config.map(KnxLightConnectionConfig::getColor)
                .map(PropertyKnxConfig::getRead)
                .ifPresent(this::unsubscribe);
    }


    private void unsubscribe(String gaStr) {
        GroupAddress ga = gaConverter.convert(gaStr);
        subscriptions.remove(ga);
        logger.debug("Unsubscribed to GA {} to receive updates", gaStr);
    }

}
