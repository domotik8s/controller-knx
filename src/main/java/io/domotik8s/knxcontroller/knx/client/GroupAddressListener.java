package io.domotik8s.knxcontroller.knx.client;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;

public interface GroupAddressListener {

    default void groupWrite(IndividualAddress source, GroupAddress destination, byte[] asdu) {}

    default void groupRead(IndividualAddress source, GroupAddress destination, byte[] asdu) {}

    default void groupReadResponse(IndividualAddress source, GroupAddress destination, byte[] asdu) {}

    boolean subscribesTo(GroupAddress address);

}