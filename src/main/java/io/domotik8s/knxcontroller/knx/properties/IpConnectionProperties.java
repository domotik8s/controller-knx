package io.domotik8s.knxcontroller.knx.properties;

import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@Getter
@Setter
public class IpConnectionProperties {

    public static enum IPConnectionType {
        ROUTING, TUNNEL
    }

    private IPConnectionType type;

    private String remoteAddress;

    private Integer remotePort = 3671;

    private String localAddress = null;

    private Integer localPort = null;

    private String localSource = "1.15.26";

    private Boolean nat = false;

}
