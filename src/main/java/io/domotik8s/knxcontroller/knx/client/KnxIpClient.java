package io.domotik8s.knxcontroller.knx.client;

import io.domotik8s.knxcontroller.knx.properties.KnxProperties;
import io.domotik8s.knxcontroller.knx.properties.IpConnectionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.InetSocketAddress;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class KnxIpClient extends KnxClientSupport {

    private final KnxProperties properties;

    @PostConstruct
    private void setUp() {
        this.connect();
    }

    @PreDestroy
    private void tearDown() {
        this.disconnect();
    }

    @Override
    public KNXNetworkLink createConnection() throws Exception {
        IpConnectionProperties config = properties.getConnection().getIp();

        // Local Socket
        String localAddress = config.getLocalAddress();
        Integer localPort = config.getLocalPort() != null ? config.getLocalPort() : 0;
        InetSocketAddress local = new InetSocketAddress(localPort);
        if (localAddress != null)
            local = new InetSocketAddress(localAddress, localPort);

        // Remote Socket
        String remoteAddr = config.getRemoteAddress();
        InetAddress remoteInetAddr = InetAddress.getByName(remoteAddr);
        int remotePort = config.getRemotePort();
        InetSocketAddress remote = new InetSocketAddress(remoteInetAddr, remotePort);

        // Settings
        String localSource = config.getLocalSource();
        KNXMediumSettings settings = KNXMediumSettings.create(KNXMediumSettings.MEDIUM_KNXIP, new IndividualAddress(localSource));

        // Network Link
        boolean nat = config.getNat();
        return KNXNetworkLinkIP.newTunnelingLink(local, remote, nat, settings);
    }


}