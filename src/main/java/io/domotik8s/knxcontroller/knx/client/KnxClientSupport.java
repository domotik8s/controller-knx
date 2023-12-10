package io.domotik8s.knxcontroller.knx.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.datapoint.StateDP;
import tuwien.auto.calimero.dptxlator.DPT;
import tuwien.auto.calimero.dptxlator.DPTXlator;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.process.ProcessCommunicator;
import tuwien.auto.calimero.process.ProcessCommunicatorImpl;
import tuwien.auto.calimero.process.ProcessEvent;
import tuwien.auto.calimero.process.ProcessListener;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public abstract class KnxClientSupport implements KnxClient, ProcessListener {

    private static Logger logger = LoggerFactory.getLogger(KnxClientSupport.class);

    @Data
    @RequiredArgsConstructor
    private static class ReadRequest {
        private final DPT dpt;
        private final GroupAddress ga;
    }

    @Data
    @RequiredArgsConstructor
    private static class WriteRequest {
        private final GroupAddress ga;
        private final DPTXlator xlator;
    }


    private KNXNetworkLink link;

    private ProcessCommunicator processCommunicator;

    private Set<GroupAddressListener> groupAddressListeners = new HashSet<>();

    private LinkedList<ReadRequest> readQueue = new LinkedList<>();

    private LinkedList<WriteRequest> writeQueue = new LinkedList<>();


    public abstract KNXNetworkLink createConnection() throws Exception;


    protected synchronized boolean connect() {
        if (isConnected()) return true;
        disconnect();
        try {
            this.link = createConnection();
            this.processCommunicator = new ProcessCommunicatorImpl(link);
            processCommunicator.addProcessListener(this);
            logger.debug("Connection to KNX established");
        } catch (Exception e) {
            logger.error("Error setting up the connection to KNX", e);
            return false;
        }
        return true;
    }

    protected synchronized void disconnect() {
        if (this.link != null) {
            this.link.close();
            this.link = null;
        }
        if (this.processCommunicator != null) {
            this.processCommunicator.removeProcessListener(this);
            this.processCommunicator.close();
            this.processCommunicator = null;
        }
        logger.debug("Connection to KNX disconnected");
    }

    private boolean isConnected() {
        boolean linkOk = link != null && link.isOpen();
        boolean pcommOk = this.processCommunicator != null;
        return linkOk && pcommOk;
    }


    /*
     * KNXClient
     */

    @Override
    public void read(DPT dpt, GroupAddress ga) {
        this.readQueue.add(new ReadRequest(dpt, ga));
    }

    @Scheduled(fixedRate = 250)
    public void readNext() {
        boolean connected = isConnected() || connect();
        if (!connected) throw new RuntimeException("Cannot read from bus since I am disconnected.");

        if (this.readQueue.isEmpty())
            return;

        ReadRequest req = this.readQueue.poll();

        if (req == null)
            return;

        assert(req.getDpt() != null);
        assert(req.getGa() != null);

        Datapoint dp = new StateDP(req.getGa(), "dafug", 0, req.getDpt().getID());

        logger.debug("Reading next from queue (1/{}): {} ({})", readQueue.size() + 1, req.getGa(), req.getDpt());
        try {
            this.processCommunicator.read(dp);
        } catch (Exception e) {
            logger.error("An error occurred reading point {} ({})", req.getGa(), req.getDpt(), e);
        }
    }

    @Override
    public void write(GroupAddress ga, DPTXlator xlator) {
        this.writeQueue.add(new WriteRequest(ga, xlator));
        logger.trace("Added write request to queue. Queue has now length of {}", this.writeQueue.size());
    }

    @Scheduled(fixedRate = 500)
    public void writeNext() {
        boolean connected = isConnected() || connect();
        if (!connected) throw new RuntimeException("Cannot write to bus since I am disconnected.");

        if (this.writeQueue.isEmpty())
            return;

        WriteRequest req = this.writeQueue.poll();

        logger.trace("Writing to KNX: {} {}", req.getGa(), req.getXlator());
        try {
            this.processCommunicator.write(req.getGa(), req.getXlator());
        } catch (KNXException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addGroupAddressListener(GroupAddressListener listener) {
        this.groupAddressListeners.add(listener);
    }


    @Override
    public void removeGroupAddressListener(GroupAddressListener listener) {
        this.groupAddressListeners.remove(listener);
    }

    /*
     * ProcessListener
     */

    @Override
    public void groupReadRequest(ProcessEvent e) {
        logger.trace("Received read request {}", e);
        this.groupAddressListeners.stream()
                .filter(l -> l.subscribesTo(e.getDestination()))
                .forEach(l -> l.groupRead(e.getSourceAddr(), e.getDestination(), e.getASDU()));
    }

    @Override
    public void groupReadResponse(ProcessEvent e) {
        logger.trace("Received read response {}", e);
        this.groupAddressListeners.stream()
                .filter(l -> l.subscribesTo(e.getDestination()))
                .forEach(l -> l.groupReadResponse(e.getSourceAddr(), e.getDestination(), e.getASDU()));
    }

    @Override
    public void groupWrite(ProcessEvent e) {
        logger.trace("Received group write {}", e);
        this.groupAddressListeners.stream()
                .filter(l -> l.subscribesTo(e.getDestination()))
                .forEach(l -> l.groupWrite(e.getSourceAddr(), e.getDestination(), e.getASDU()));
    }

    @Override
    public void detached(DetachEvent e) {
        logger.debug("Network link detached.");
    }

}
