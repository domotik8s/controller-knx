package io.domotik8s.knxcontroller.k8s.ctrl.numericsensor.model;

import lombok.Data;

@Data
public class KnxNumericSensorConnectionConfig {

    private String address;

    private String dpt;

}
