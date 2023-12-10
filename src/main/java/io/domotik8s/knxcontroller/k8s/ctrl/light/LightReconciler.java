package io.domotik8s.knxcontroller.k8s.ctrl.light;

import io.domotik8s.knxcontroller.k8s.ctrl.AbstractReconciler;
import io.domotik8s.knxcontroller.k8s.ctrl.model.AddressPair;
import io.domotik8s.knxcontroller.k8s.ctrl.light.model.V1Beta1KnxLight;
import io.domotik8s.knxcontroller.k8s.ctrl.light.model.V1Beta1KnxLightList;
import io.domotik8s.knxcontroller.k8s.ctrl.light.model.V1Beta1LightKnxConnectionConfig;
import io.domotik8s.knxcontroller.knx.client.KnxClient;
import io.domotik8s.knxcontroller.knx.convert.StringToDptConverter;
import io.domotik8s.knxcontroller.knx.convert.StringToGroupAddressConverter;
import io.domotik8s.model.*;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.dptxlator.DPT;
import tuwien.auto.calimero.dptxlator.DPTXlatorBoolean;
import tuwien.auto.calimero.dptxlator.TranslatorTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class LightReconciler extends AbstractReconciler<V1Beta1KnxLight, V1Beta1KnxLightList>  {

    @Autowired
    @Getter
    private SharedIndexInformer<V1Beta1KnxLight> informer;

    @Autowired
    private GenericKubernetesApi<V1Beta1KnxLight, V1Beta1KnxLightList> lightClient;

    @Autowired
    private KnxClient knxClient;


    @Override
    public GenericKubernetesApi<V1Beta1KnxLight, V1Beta1KnxLightList> getClient() {
        return lightClient;
    }

    @Override
    public void reconcileState(V1Beta1KnxLight light) throws KNXException {
        V1beta1LightState current = Optional.ofNullable(light.getStatus()).map(V1beta1LightStatus::getState).orElse(new V1beta1LightState());
        V1beta1LightState desired = Optional.ofNullable(light.getSpec()).map(V1beta1LightSpec::getState).orElse(new V1beta1LightState());

        List<String> unknowns = new ArrayList<>();

        boolean updatePower = shouldUpdate(desired.getPower(), current.getPower());
        if (updatePower) {
            getLogger().debug("Light {} has power state {} but should be {}", light.getMetadata().getName(), current.getPower(), desired.getPower());
            updatePower(light.getSpec().getConnection().getConfig(), desired.getPower());

        // If we're not updating the state anyway and the current state is empty, send read request to KNX
        } else if (Optional.ofNullable(current.getPower()).isEmpty()) {
            Optional<AddressPair> powerConfig = Optional.ofNullable(light)
                    .map(V1Beta1KnxLight::getSpec)
                    .map(V1beta1LightSpec::getConnection)
                    .map(V1beta1LightSpecConnection::getConfig)
                    .map(V1Beta1LightKnxConnectionConfig::getPower);

            if (powerConfig.isPresent() && powerConfig.get().getRead() != null && powerConfig.get().getDpt() != null) {
                GroupAddress ga = ((new StringToGroupAddressConverter()).convert(powerConfig.get().getRead()));
                DPT dpt = ((new StringToDptConverter()).convert(powerConfig.get().getDpt()));
                getLogger().debug("Request status update from KNX for GA {}", ga);
                knxClient.read(dpt, ga);
            }
        }

    }

    private void updatePower(V1Beta1LightKnxConnectionConfig config, boolean value) throws KNXException {
        AddressPair powerCfg = config.getPower();

        StringToGroupAddressConverter gaConverter = new StringToGroupAddressConverter();
        GroupAddress writeGA = gaConverter.convert(powerCfg.getWrite());

        StringToDptConverter dptConverter = new StringToDptConverter();
        DPT writeDPT = dptConverter.convert(powerCfg.getDpt());

        DPTXlatorBoolean xlator = (DPTXlatorBoolean) TranslatorTypes.createTranslator(0, writeDPT.getID());
        xlator.setValue(value);

        getLogger().debug("Sending to GA {}: {}", writeGA, value);
        knxClient.write(writeGA, xlator);
    }


}
