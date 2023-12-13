package io.domotik8s.knxcontroller.k8s.ctrl.model;

import lombok.Data;

@Data
public class PropertyKnxConfig {

    private String read;

    private String write;

    private String dpt;

}
