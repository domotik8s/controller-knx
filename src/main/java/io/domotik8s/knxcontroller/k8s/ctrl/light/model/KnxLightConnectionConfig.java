package io.domotik8s.knxcontroller.k8s.ctrl.light.model;

import io.domotik8s.knxcontroller.k8s.ctrl.model.PropertyKnxConfig;
import lombok.Data;

@Data
public class KnxLightConnectionConfig {

    private PropertyKnxConfig power;

    private PropertyKnxConfig brightness;

    private PropertyKnxConfig color;


}
