package org.matsim.run.replaceCarByDRT;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.w3c.dom.html.HTMLDocument;

import java.util.HashMap;
import java.util.Map;

public class TrafficVolumeEventHandler implements LinkEnterEventHandler {

    private static final Logger log = LogManager.getLogger(TrafficVolumeEventHandler.class);

    private Map<Id<Link>, Integer> DTVPerLink_Total = new HashMap<>();
    private Map<Id<Link>, Double> MileagePerLink = new HashMap<>();

    private Network network;

    //TODO: nicht nur LinkEnterEvent, aber noch ein zweites Event, siehe Google Doc

    public TrafficVolumeEventHandler(Network network) {
        for (Id<Link> linkId : network.getLinks().keySet()){
            if(!String.valueOf(linkId).contains("pt")){
                this.DTVPerLink_Total.put(linkId,0);
                this.MileagePerLink.put(linkId,0.0);
            }
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<Link> linkId = event.getLinkId();

        if(!String.valueOf(linkId).contains("pt")){
            DTVPerLink_Total.put(linkId, DTVPerLink_Total.get(linkId) + 1);
        }

    }

    public Map<Id<Link>, Integer> getDTVPerLink_Total() {
        return DTVPerLink_Total;
    }
}
