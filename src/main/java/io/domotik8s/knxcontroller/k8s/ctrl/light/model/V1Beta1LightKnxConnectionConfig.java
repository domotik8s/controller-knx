package io.domotik8s.knxcontroller.k8s.ctrl.light.model;

import io.domotik8s.knxcontroller.k8s.ctrl.model.AddressPair;
import lombok.Data;

@Data
public class V1Beta1LightKnxConnectionConfig {

    private AddressPair power;

    private AddressPair brightness;

    private AddressPair color;


}
