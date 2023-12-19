package io.domotik8s.knxcontroller.k8s.syncer;

import io.domotik8s.knxcontroller.k8s.model.KnxBooleanProperty;
import io.domotik8s.knxcontroller.k8s.model.KnxBooleanPropertySpec;
import io.domotik8s.knxcontroller.k8s.model.KnxPropertyAddress;
import io.domotik8s.knxcontroller.knx.client.GroupAddressListener;
import io.domotik8s.knxcontroller.knx.client.KnxClient;
import io.domotik8s.knxcontroller.knx.convert.StringToDptConverter;
import io.domotik8s.knxcontroller.knx.convert.StringToGroupAddressConverter;
import io.domotik8s.model.Property;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public abstract class AbstractSyncer<T extends Property<?, ?>> implements ResourceEventHandler<T>, GroupAddressListener {

    @Getter(AccessLevel.PROTECTED)
    private Logger logger = LoggerFactory.getLogger(getClass());


    @Getter(AccessLevel.PROTECTED)
    private final KnxClient knxClient;

    @Getter(AccessLevel.PROTECTED)
    private final SharedIndexInformer<T> informer;


    @Getter(AccessLevel.PROTECTED)
    private final StringToGroupAddressConverter gaConverter = new StringToGroupAddressConverter();

    @Getter(AccessLevel.PROTECTED)
    private final StringToDptConverter dptConverter = new StringToDptConverter();


    @PostConstruct
    private void register() {
        knxClient.addGroupAddressListener(this);
        informer.addEventHandler(this);
    }


    public abstract void updateCurrentState(GroupAddress ga, byte[] asdu);

    public abstract Optional<String> extractReadAddress(T property);



    /*
    Subscription
     */

    private final Set<GroupAddress> subscriptions = new HashSet<>();

    private void subscribe(T property) {
        Optional<String> readAddrStr = extractReadAddress(property);
        if (readAddrStr.isPresent()) {
            GroupAddress ga = gaConverter.convert(readAddrStr.get());
            subscriptions.add(ga);
            logger.debug("Subscribed to GA {} to receive power updates", readAddrStr.get());
        }
    }

    private void unsubscribe(T property) {
        Optional<String> readAddrStr = extractReadAddress(property);
        if (readAddrStr.isPresent()) {
            GroupAddress ga = gaConverter.convert(readAddrStr.get());
            subscriptions.remove(ga);
            logger.debug("Unsubscribed to GA {} to receive updates", readAddrStr.get());
        }
    }

    /*
    GroupListener
     */

    @Override
    public void groupWrite(IndividualAddress source, GroupAddress destination, byte[] asdu) {
        updateCurrentState(destination, asdu);
    }

    @Override
    public void groupRead(IndividualAddress source, GroupAddress destination, byte[] asdu) {
        // Ignore
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
    ResourceEventHandler
     */

    @Override
    public void onAdd(T property) {
        subscribe(property);
    }

    @Override
    public void onUpdate(T property, T apiType1) {
        subscribe(property);
    }

    @Override
    public void onDelete(T property, boolean b) {
        unsubscribe(property);
    }

}
