package io.domotik8s.knxcontroller.k8s.ctrl.sensor.numeric.model;

import lombok.Data;

@Data
public class KnxNumericSensorConnectionConfig {

    private String address;

    private String dpt;

}
