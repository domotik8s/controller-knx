package io.domotik8s.knxcontroller.manager;

import io.domotik8s.model.V1beta1Light;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tuwien.auto.calimero.GroupAddress;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//@Component
//@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LightManager { // implements DeviceManager<V1beta1Light> {

    private Logger logger = LoggerFactory.getLogger(LightManager.class);


    // private final KnxClient knxClient;

    private final Set<GroupAddress> subscribedGroupAddresses = new HashSet<>();
    private final Map<GroupAddress, V1beta1Light> gaToLightMap = new HashMap<>();

    /*
     * K8sEventHandler
     */

//    @Override
//    public void handleAdded(V1beta1Light light) {
//        logger.info("Light was added: {}", light.getMetadata().getName());
//
//        // Update resource capabilities based on configured group addresses
//        boolean updated = updateCapabilities(light);
//        boolean copied = copyCurrentState2DesiredState(light);
//        if (updated == true || copied == true) {
//            k8sClient.update(light);
//        }
//
//        // Add all specified read addresses to the list of subscribed addresses
//        // so that we get related events
//        Optional<V1Beta1LightConnectionConfig> config = Optional.ofNullable(light)
//                .map(V1beta1Light::getSpec)
//                .map(V1beta1LightSpec::getConnection)
//                .map(V1beta1LightSpecConnection::getConfig);
//
//        Optional<V1Beta1LightConnectionConfig.AddressPair> powerConfig = config.map(V1Beta1LightConnectionConfig::getPower);
//        Optional<V1Beta1LightConnectionConfig.AddressPair> brightnessConfig = config.map(V1Beta1LightConnectionConfig::getBrightness);
//        Optional<V1Beta1LightConnectionConfig.AddressPair> colorConfig = config.map(V1Beta1LightConnectionConfig::getColor);
//
//        if (powerConfig.isPresent()) {
//            V1Beta1LightConnectionConfig.AddressPair pair = powerConfig.get();
//
//            if (pair.getRead() != null) {
//                // Get the GroupAddress
//                String readGaString = pair.getRead();
//                StringToGroupAddressConverter gaConverter = new StringToGroupAddressConverter();
//                GroupAddress ga = gaConverter.convert(readGaString);
//
//                // Add to internal data structures
//                gaToLightMap.put(ga, light);
//                subscribedGroupAddresses.add(ga);
//
//                // Get the DPT
//                String dptString = pair.getDpt();
//                StringToDptConverter dptConverter = new StringToDptConverter();
//                DPT dpt = dptConverter.convert(dptString);
//
//                // Trigger update on KNX bus for device
//                logger.debug("Reading from GA {} with DPT {}", readGaString, dptString);
//                knxClient.read(dpt, ga);
//            }
//        }
//    }
//
//    @Override
//    public void handleModified(V1beta1Light before, V1beta1Light after) {
//        logger.info("Light was modified: {}", after.getMetadata().getName());
//
//        Optional<Boolean> currentPower = Optional.ofNullable(before.getStatus())
//                .map(V1beta1LightStatus::getState)
//                .map(V1beta1LightState::getPower);
//
//        Optional<Boolean> desiredPower = Optional.ofNullable(after.getSpec())
//                .map(V1beta1LightSpec::getState)
//                .map(V1beta1LightState::getPower);
//
//        if (desiredPower.isPresent() && !desiredPower.get().equals(currentPower.get())) {
//            Boolean powerValue = desiredPower.get();
//
//            logger.debug("Updating light {} power to {}", after.getMetadata().getName(), powerValue);
//
//            Optional<String> powerWriteAddr = Optional.ofNullable(after.getSpec())
//                .map(V1beta1LightSpec::getConnection)
//                .map(V1beta1LightSpecConnection::getConfig)
//                .map(V1Beta1LightConnectionConfig::getPower)
//                .map(V1Beta1LightConnectionConfig.AddressPair::getWrite);
//
//            Optional<String> powerDpt = Optional.ofNullable(after.getSpec())
//                    .map(V1beta1LightSpec::getConnection)
//                    .map(V1beta1LightSpecConnection::getConfig)
//                    .map(V1Beta1LightConnectionConfig::getPower)
//                    .map(V1Beta1LightConnectionConfig.AddressPair::getDpt);
//
//            if (powerWriteAddr.isPresent()) {
//                StringToGroupAddressConverter gaConverter = new StringToGroupAddressConverter();
//                GroupAddress writeGA = gaConverter.convert(powerWriteAddr.get());
//
//                StringToDptConverter dptConverter = new StringToDptConverter();
//                DPT writeDPT = dptConverter.convert(powerDpt.get());
//
//                DPTXlatorBoolean xlator = null;
//                try {
//                    xlator = (DPTXlatorBoolean) TranslatorTypes.createTranslator(0, writeDPT.getID());
//                } catch (KNXException e) {
//                    throw new RuntimeException(e);
//                }
//                if (xlator == null)
//                    throw new RuntimeException("State is of type boolean type but could not load translator based on DPT \" + dpt.getID()");
//
//                xlator.setValue(powerValue);
//
//                knxClient.write(writeGA, xlator);
//            }
//
//        }
//    }
//
//    @Override
//    public void handleDeleted(V1beta1Light light) {
//        logger.info("Light was deleted: {}", light.getMetadata().getName());
//    }
//
//    @Override
//    public Class<V1beta1Light> subscribedType() {
//        return V1beta1Light.class;
//    }
//
//
//    /*
//     * KnxEventHandler
//     */
//
//
//    /**
//     * When the state changes on the KNX bus, we need to
//     * 1. copy the current state to the desired state if no desired state is known
//     * 2. update the current state
//     * 3. update the desired state with the same value if not enforced
//     *
//     * @param destination
//     * @param data
//     */
//    @Override
//    public void handleGroupAddressUpdate(GroupAddress destination, byte[] data) {
//        logger.info("Received update for GA {}: {}", destination, data);
//
//        V1beta1Light light = gaToLightMap.get(destination);
//
//        // If we don't know about any light that uses this group address, skip it
//        if (light == null) {
//            logger.debug("Received update but cannot find related light");
//            return;
//        }
//
//        // If the status object is null, create it
//        Optional<V1beta1LightStatus> status = Optional.ofNullable(light).map(V1beta1Light::getStatus);
//        if (status.isEmpty()) {
//            V1beta1LightStatus statusValue = new V1beta1LightStatus();
//            light.setStatus(statusValue);
//            status = Optional.of(statusValue);
//        }
//
//        // If the statue.state object is null, create it
//        Optional<V1beta1LightState> state = status.map(V1beta1LightStatus::getState);
//        if (state.isEmpty()) {
//            V1beta1LightState stateValue = new V1beta1LightState();
//            status.get().setState(stateValue);
//        }
//
//        // Prepare the destination group address as a string, to subsequence methods
//        // can perform a simple string comparison with the GA mentioned in the config
//        GroupAddressToStringConverter ga2Str = new GroupAddressToStringConverter();
//        String destGA = ga2Str.convert(destination);
//
//        // Handle different state properties in their own methods
//        List<Boolean> statusUpdates = List.of(
//            updatePower(light, destGA, data),
//            false, // brightness
//            false // color
//        );
//
//        // Only update the status if anything has changed
//        if (statusUpdates.contains(true)) {
//            logger.info("Updating status of light {}", light.getMetadata().getName());
//            status.get().setLastUpdated(OffsetDateTime.now());
//            k8sClient.updateStatus(light, light.getStatus());
//        }
//
//        if (statusUpdates.contains(true) && Boolean.FALSE.equals(light.getSpec().getEnforce())) {
//            light = k8sClient.fetchLatest(light);
//            boolean copied = copyCurrentState2DesiredState(light);
//            if (copied == true) {
//                k8sClient.update(light);
//            }
//        }
//    }
//
//    private boolean copyCurrentState2DesiredState(V1beta1Light light) {
//        // If no desired state is set, assume the current state is the desired state
//        // and copy the values over
//        Optional<V1beta1LightState> currentState = Optional.ofNullable(light.getStatus())
//                .map(V1beta1LightStatus::getState);
//
//        if (currentState.isPresent()) {
//            light.getSpec().setState(currentState.get());
//            return true;
//        }
//        return false;
//    }
//
//    private boolean updatePower(V1beta1Light light, String groupAddress, byte[] data) {
//        Optional<V1Beta1LightConnectionConfig.AddressPair> powerConfig = Optional.ofNullable(light.getSpec())
//                .map(V1beta1LightSpec::getConnection)
//                .map(V1beta1LightSpecConnection::getConfig)
//                .map(V1Beta1LightConnectionConfig::getPower);
//
//        if (!groupAddress.equals(powerConfig.get().getRead())) return false;
//
//        logger.debug("Message relates to the power property of light " + light.getMetadata().getName());
//
//        String dptString = powerConfig.get().getDpt();
//        StringToDptConverter dptConverter = new StringToDptConverter();
//        DPT dpt = dptConverter.convert(dptString);
//
//        try {
//            DPTXlator xlator = TranslatorTypes.createTranslator(dpt, data);
//            DPTXlatorBoolean boolXlator = (DPTXlatorBoolean) xlator;
//            Boolean value = boolXlator.getValueBoolean();
//
//            V1beta1LightStatus status = light.getStatus();
//            V1beta1LightSpec spec = light.getSpec();
//
//            Optional<V1beta1LightState> currentState = Optional.ofNullable(status.getState());
//            Optional<V1beta1LightState> desiredState = Optional.ofNullable(spec.getState());
//
//            if (currentState.isEmpty()) {
//                V1beta1LightState stateValue = new V1beta1LightState();
//                status.setState(stateValue);
//                currentState = Optional.of(stateValue);
//            }
//
//            logger.debug("Updating current power state to {}", value);
//            currentState.get().setPower(value);
//
//            if (desiredState.isPresent()) {
//                logger.debug("Updating desired power state to {}", value);
//                desiredState.get().setPower(value);
//            }
//
//            return true;
//        } catch (KNXException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
//
//
//    @Override
//    public Set<GroupAddress> subscribedGroupAddresses() {
//        return subscribedGroupAddresses;
//    }
//
//
//    /*
//     * Helpers
//     */
//
//    private boolean updateCapabilities(V1beta1Light light) {
//        V1Beta1LightConnectionConfig config = light.getSpec().getConnection().getConfig();
//
//        boolean powerCap = readWriteSet(config.getPower());
//        boolean brightnessCap = readWriteSet(config.getBrightness());
//        boolean colorCap = readWriteSet(config.getColor());
//
//        V1beta1LightSpecCapabilities caps = Optional.ofNullable(light.getSpec())
//                .map(V1beta1LightSpec::getCapabilities)
//                .orElse(new V1beta1LightSpecCapabilities());
//
//        caps.setPower(powerCap);
//        caps.setBrightness(brightnessCap);
//        caps.setColor(colorCap);
//
//        light.getSpec().setCapabilities(caps);
//        return true;
//    }
//
//    private boolean readWriteSet(V1Beta1LightConnectionConfig.AddressPair pair) {
//        if (pair == null) return false;
//        return pair.getRead() != null && pair.getWrite() != null;
//    }

}
