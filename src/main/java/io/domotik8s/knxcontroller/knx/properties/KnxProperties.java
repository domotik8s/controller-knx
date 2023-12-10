package io.domotik8s.knxcontroller.knx.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "knx")
public class KnxProperties {

    private String instance;

    private ConnectionProperties connection;

}