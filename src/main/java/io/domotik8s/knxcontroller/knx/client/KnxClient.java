package io.domotik8s.knxcontroller.knx.client;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.dptxlator.DPT;
import tuwien.auto.calimero.dptxlator.DPTXlator;

public interface KnxClient {

    void read(DPT dpt, GroupAddress ga);

    void write(GroupAddress ga, DPTXlator xlator);


    void addGroupAddressListener(GroupAddressListener listener);

    void removeGroupAddressListener(GroupAddressListener listener);

}