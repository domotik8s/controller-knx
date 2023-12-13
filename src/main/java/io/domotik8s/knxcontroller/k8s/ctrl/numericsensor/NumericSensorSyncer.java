package io.domotik8s.knxcontroller.k8s.ctrl.numericsensor;

import io.domotik8s.knxcontroller.k8s.ctrl.numericsensor.model.KnxNumericSensor;
import io.domotik8s.knxcontroller.k8s.ctrl.numericsensor.model.KnxNumericSensorConnectionConfig;
import io.domotik8s.knxcontroller.k8s.ctrl.numericsensor.model.KnxNumericSensorList;
import io.domotik8s.knxcontroller.knx.client.GroupAddressListener;
import io.domotik8s.knxcontroller.knx.client.KnxClient;
import io.domotik8s.knxcontroller.knx.convert.StringToDptConverter;
import io.domotik8s.knxcontroller.knx.convert.StringToGroupAddressConverter;
import io.domotik8s.knxcontroller.knx.utils.DptUnitConverter;
import io.domotik8s.model.numericsensor.NumericSensorSpec;
import io.domotik8s.model.numericsensor.NumericSensorSpecConnection;
import io.domotik8s.model.numericsensor.NumericSensorState;
import io.domotik8s.model.numericsensor.NumericSensorStatus;
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
import javax.measure.Unit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NumericSensorSyncer implements ResourceEventHandler<KnxNumericSensor>, GroupAddressListener {

    private final Logger logger = LoggerFactory.getLogger(NumericSensorSyncer.class);


    private final SharedIndexInformer<KnxNumericSensor> informer;

    private final GenericKubernetesApi<KnxNumericSensor, KnxNumericSensorList> sensorClient;

    private final KnxClient knxClient;

    private final Set<GroupAddress> subscriptions = new HashSet<>();

    private final StringToGroupAddressConverter gaConverter = new StringToGroupAddressConverter();

    private final StringToDptConverter dptConverter = new StringToDptConverter();

    private final DptUnitConverter unitConverter = new DptUnitConverter();


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
        updateState(destination, asdu);
    }

    @Override
    public void groupReadResponse(IndividualAddress source, GroupAddress destination, byte[] asdu) {
        logger.debug("Received read response on KNX bus to address {} with asdu {}", destination, asdu);
        updateState(destination, asdu);
    }

    @Override
    public boolean subscribesTo(GroupAddress address) {
        return subscriptions.contains(address);
    }


    /*
     * ResourceEventHandler (Calls from K8s)
     */

    @Override
    public void onAdd(KnxNumericSensor sensor) {
        subscribe(sensor);

        Optional<KnxNumericSensorConnectionConfig> config = Optional.ofNullable(sensor)
                .map(KnxNumericSensor::getSpec)
                .map(NumericSensorSpec::getConnection)
                .map(NumericSensorSpecConnection::getConfig);

        if (config.isEmpty()) return;

        GroupAddress ga = gaConverter.convert(config.get().getAddress());
        DPT dpt = dptConverter.convert(config.get().getDpt());
        logger.debug("Request status update from KNX for GA {}", ga);
        knxClient.read(dpt, ga);
    }

    @Override
    public void onUpdate(KnxNumericSensor before, KnxNumericSensor after) {
        subscribe(after);
    }

    @Override
    public void onDelete(KnxNumericSensor sensor, boolean b) {
        unsubscribe(sensor);
    }


    /*
     * Syncer Methods
     */

    private void updateState(GroupAddress destination, byte[] asdu) {
        logger.debug("Received update for GA {} with value {}", destination, asdu);

        KubernetesApiResponse<KnxNumericSensorList> listResp = sensorClient.list();
        KnxNumericSensorList list = listResp.getObject();

        for (KnxNumericSensor sensor: list.getItems()) {
            Optional<KnxNumericSensorConnectionConfig> config = Optional.ofNullable(sensor.getSpec())
                    .map(NumericSensorSpec::getConnection)
                    .map(NumericSensorSpecConnection::getConfig);

            if (config.isEmpty()) continue;

            GroupAddress address = gaConverter.convert(config.get().getAddress());
            DPT dpt = dptConverter.convert(config.get().getDpt());

            if (destination.equals(address)) {
                DPTXlator xlator = null;
                try {
                    xlator = TranslatorTypes.createTranslator(dpt, asdu);
                } catch (KNXException e) {
                    throw new RuntimeException(e);
                }
                DPTXlator2ByteFloat floatXlator = (DPTXlator2ByteFloat) xlator;
                double value = floatXlator.getNumericValue();

                Unit unit = unitConverter.toUnit(dpt);

                NumericSensorStatus status = Optional.ofNullable(sensor.getStatus()).orElse(new NumericSensorStatus());
                sensor.setStatus(status);

                NumericSensorState state = Optional.ofNullable(status.getState()).orElse(new NumericSensorState());
                status.setState(state);

                state.setValue(value);
                state.setUnit(unit.toString());

                sensorClient.updateStatus(sensor, (l) -> l.getStatus());
                break;
            }
        }
    }

    private void subscribe(KnxNumericSensor sensor) {
        Optional<KnxNumericSensorConnectionConfig> config = Optional.ofNullable(sensor.getSpec())
                .map(NumericSensorSpec::getConnection)
                .map(NumericSensorSpecConnection::getConfig);

        if (config.isEmpty()) {
            logger.warn("Unable to subscribe to sensor {}. Not value found for spec.connection.config", sensor.getMetadata().getName());
            return;
        }

        config.map(KnxNumericSensorConnectionConfig::getAddress)
                .ifPresent((value) -> {
                    GroupAddress ga = gaConverter.convert(value);
                    subscriptions.add(ga);
                    logger.debug("Subscribed to GA {} to receive power updates", value);
                });
    }

    private void unsubscribe(KnxNumericSensor sensor) {
        Optional<KnxNumericSensorConnectionConfig> config = Optional.ofNullable(sensor.getSpec())
                .map(NumericSensorSpec::getConnection)
                .map(NumericSensorSpecConnection::getConfig);

        if (config.isEmpty()) {
            logger.warn("Unable to unsubscribe from sensor {}. Not value found for spec.connection.config", sensor.getMetadata().getName());
            return;
        }

        config.map(KnxNumericSensorConnectionConfig::getAddress)
                .ifPresent((value) -> {
                    GroupAddress ga = gaConverter.convert(value);
                    subscriptions.remove(ga);
                    logger.debug("Unsubscribed to GA {} to receive updates", value);
                });

    }

}
