package org.sar.ppi.communication.AppEvents;

import org.sar.ppi.peersim.PeerSimInfrastructure;

public class OffEvent implements AppEvent {
    private PeerSimInfrastructure peerInfra;

    public OffEvent(PeerSimInfrastructure p){peerInfra=p;}

    @Override
    public void run() {
        peerInfra.undeploy();
    }
}