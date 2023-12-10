package io.domotik8s.knxcontroller.knx.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class ConnectionProperties {

    private IpConnectionProperties ip;

}