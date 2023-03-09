package org.matsim.events.CongestionDetection;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

/**
 * This EventHandler implementation counts the travel time of
 * all agents and provides the average travel time per
 * agent.
 * Actually, handling Departures and Arrivals should be sufficient for this (may 2014)
 * @author dgrether
 *
 */

public class CongestionDetectionEventHandler implements LinkEnterEventHandler,
        LinkLeaveEventHandler{

    private Map<Id<Vehicle>,Double> earliestLinkExitTime = new HashMap<>() ;
    private Network network;

    private HashMap<String, String> prStationsMap = new HashMap<String, String>();

    public CongestionDetectionEventHandler(Network network) {
        this.network = network ;
    }

    @Override
    public void reset(int iteration) {
        this.earliestLinkExitTime.clear();
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        for (Map.Entry<String, String> entry : getPrStationsMap().entrySet()) {
            if (event.getLinkId().equals(Id.createLinkId(entry.getValue()))) {
                Link link = network.getLinks().get(event.getLinkId());
                double linkTravelTime = link.getLength() / link.getFreespeed(event.getTime());
                this.earliestLinkExitTime.put(event.getVehicleId(), event.getTime() + linkTravelTime);
            }
        }
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        for (Map.Entry<String, String> entry : getPrStationsMap().entrySet()) {
            if (event.getLinkId().equals(Id.createLinkId(entry.getValue()))) {
                double excessTravelTime = event.getTime() - this.earliestLinkExitTime.get(event.getVehicleId());
                System.out.println("excess travel time: " + excessTravelTime);
            }
        }
    }

    public HashMap<String, String> getPrStationsMap() {
        return prStationsMap;
    }

    public void setPrStationsMap(HashMap<String, String> prStationsMap) {
        this.prStationsMap = prStationsMap;
    }
}
