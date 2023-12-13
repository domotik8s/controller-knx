package io.domotik8s.knxcontroller.k8s.ctrl.light;

import io.domotik8s.knxcontroller.k8s.K8sConstants;
import io.domotik8s.knxcontroller.k8s.ctrl.AbstractReconciler;
import io.domotik8s.knxcontroller.k8s.ctrl.model.PropertyKnxConfig;
import io.domotik8s.knxcontroller.k8s.ctrl.light.model.KnxLight;
import io.domotik8s.knxcontroller.k8s.ctrl.light.model.KnxLightList;
import io.domotik8s.knxcontroller.k8s.ctrl.light.model.KnxLightConnectionConfig;
import io.domotik8s.knxcontroller.knx.client.KnxClient;
import io.domotik8s.knxcontroller.knx.convert.StringToDptConverter;
import io.domotik8s.knxcontroller.knx.convert.StringToGroupAddressConverter;
import io.domotik8s.knxcontroller.knx.properties.KnxProperties;
import io.domotik8s.model.light.LightSpec;
import io.domotik8s.model.light.LightSpecConnection;
import io.domotik8s.model.light.LightState;
import io.domotik8s.model.light.LightStatus;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.dptxlator.DPT;
import tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned;
import tuwien.auto.calimero.dptxlator.DPTXlatorBoolean;
import tuwien.auto.calimero.dptxlator.TranslatorTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class LightReconciler extends AbstractReconciler<KnxLight, KnxLightList>  {

    @Autowired
    @Getter
    private SharedIndexInformer<KnxLight> informer;

    @Autowired
    private GenericKubernetesApi<KnxLight, KnxLightList> lightClient;

    @Autowired
    private KnxClient knxClient;

    @Autowired
    private KnxProperties properties;

    @Override
    public GenericKubernetesApi<KnxLight, KnxLightList> getClient() {
        return lightClient;
    }


    /*
     * Filtering
     */

    public boolean onAddFilter(KnxLight light) {
        return filterSystemAndInstance(light);
    }

    public boolean onUpdateFilter(KnxLight oldLight, KnxLight newLight) {
        return filterSystemAndInstance(newLight);
    }

    public boolean onDeleteFilter(KnxLight light, Boolean value) {
        return filterSystemAndInstance(light);
    }

    private boolean filterSystemAndInstance(KnxLight light) {
        Optional<String> resourceSystemType = Optional.ofNullable(light.getSpec()).map(LightSpec::getConnection).map(LightSpecConnection::getSystem);
        boolean typeMatch = K8sConstants.SYSTEM_TYPE.equals(resourceSystemType.get());

        Optional<String> thisInstance = Optional.ofNullable(properties.getInstance());
        Optional<String> resourceInstance = Optional.ofNullable(light.getSpec()).map(LightSpec::getConnection).map(LightSpecConnection::getSystem);

        boolean instanceMatch = true;
        if (thisInstance.isPresent()) {
            instanceMatch = thisInstance.get().equals(resourceInstance.orElse(null));
        }

        return typeMatch && instanceMatch;
    }

    /*
     * Reconciliation
     */

    @Override
    public void reconcileState(KnxLight light) throws KNXException {
        LightState current = Optional.ofNullable(light.getStatus()).map(LightStatus::getState).orElse(new LightState());
        LightState desired = Optional.ofNullable(light.getSpec()).map(LightSpec::getState).orElse(new LightState());

        List<String> unknowns = new ArrayList<>();

        boolean updatePower = shouldUpdate(desired.getPower(), current.getPower());
        if (updatePower) {
            getLogger().debug("Light {} has power state {} but should be {}", light.getMetadata().getName(), current.getPower(), desired.getPower());
            updatePower(light.getSpec().getConnection().getConfig(), desired.getPower());
        // If we're not updating the state anyway and the current state is empty, send read request to KNX
        } else if (Optional.ofNullable(current.getPower()).isEmpty()) {
            Optional<PropertyKnxConfig> powerConfig = Optional.ofNullable(light)
                    .map(KnxLight::getSpec)
                    .map(LightSpec::getConnection)
                    .map(LightSpecConnection::getConfig)
                    .map(KnxLightConnectionConfig::getPower);

            if (powerConfig.isPresent() && powerConfig.get().getRead() != null && powerConfig.get().getDpt() != null) {
                GroupAddress ga = ((new StringToGroupAddressConverter()).convert(powerConfig.get().getRead()));
                DPT dpt = ((new StringToDptConverter()).convert(powerConfig.get().getDpt()));
                getLogger().debug("Request status update from KNX for GA {}", ga);
                knxClient.read(dpt, ga);
            }
        }


        boolean updateBrightness = shouldUpdate(desired.getBrightness(), current.getBrightness());
        if (updateBrightness) {
            getLogger().debug("Light {} has brightness {} but should be {}", light.getMetadata().getName(), current.getBrightness(), desired.getBrightness());
            updateBrightness(light.getSpec().getConnection().getConfig(), desired.getBrightness());
            // If we're not updating the state anyway and the current state is empty, send read request to KNX
        } else if (Optional.ofNullable(current.getBrightness()).isEmpty()) {
            Optional<PropertyKnxConfig> brightnessConfig = Optional.ofNullable(light)
                    .map(KnxLight::getSpec)
                    .map(LightSpec::getConnection)
                    .map(LightSpecConnection::getConfig)
                    .map(KnxLightConnectionConfig::getBrightness);

            if (brightnessConfig.isPresent() && brightnessConfig.get().getRead() != null && brightnessConfig.get().getDpt() != null) {
                GroupAddress ga = ((new StringToGroupAddressConverter()).convert(brightnessConfig.get().getRead()));
                DPT dpt = ((new StringToDptConverter()).convert(brightnessConfig.get().getDpt()));
                getLogger().debug("Request status update from KNX for GA {}", ga);
                knxClient.read(dpt, ga);
            }
        }

    }

    private void updatePower(KnxLightConnectionConfig config, boolean value) throws KNXException {
        PropertyKnxConfig powerCfg = config.getPower();

        StringToGroupAddressConverter gaConverter = new StringToGroupAddressConverter();
        GroupAddress writeGA = gaConverter.convert(powerCfg.getWrite());

        StringToDptConverter dptConverter = new StringToDptConverter();
        DPT writeDPT = dptConverter.convert(powerCfg.getDpt());

        DPTXlatorBoolean xlator = (DPTXlatorBoolean) TranslatorTypes.createTranslator(0, writeDPT.getID());
        xlator.setValue(value);

        getLogger().debug("Sending to GA {}: {}", writeGA, value);
        knxClient.write(writeGA, xlator);
    }


    private void updateBrightness(KnxLightConnectionConfig config, double value) throws KNXException {
        PropertyKnxConfig brightnessCfg = config.getBrightness();

        StringToGroupAddressConverter gaConverter = new StringToGroupAddressConverter();
        GroupAddress writeGA = gaConverter.convert(brightnessCfg.getWrite());

        StringToDptConverter dptConverter = new StringToDptConverter();
        DPT writeDPT = dptConverter.convert(brightnessCfg.getDpt());

        DPTXlator8BitUnsigned xlator = (DPTXlator8BitUnsigned) TranslatorTypes.createTranslator(0, writeDPT.getID());
        xlator.setValue(value / 100);

        getLogger().debug("Sending to GA {}: {}", writeGA, value / 100);
        knxClient.write(writeGA, xlator);
    }

}
