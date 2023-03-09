package org.matsim.events.parkAndRideEvents;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * This event handler prints some event information to the console.
 * @author dgrether
 *
 * TO DO:
 * Filter by Vehicle type (only car & DRT) -> not necessary bcz. no PT/other modes on these links anyway
 *
 */

public class parkAndRideEventHandler implements
        LinkLeaveEventHandler, LinkEnterEventHandler {

    private HashMap<String, String> prStationsLinkID = new HashMap<String, String>();

    private HashMap<String, Double> agentsPerPRStationLink = new HashMap<String, Double>();

    @Override
    public void reset(int iteration) {
        System.out.println("reset...");
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {

        for (Map.Entry<String, String> entry : prStationsLinkID.entrySet()) {
            if (event.getLinkId().equals(Id.createLinkId(entry.getValue()))){
                // increase value of key by 1
                agentsPerPRStationLink.putIfAbsent(entry.getKey(),0.0);
                agentsPerPRStationLink.put(entry.getKey(),agentsPerPRStationLink.get(entry.getKey()) + 1.0);
            }
        }
    }

    //tbd
    @Override
    public void handleEvent(LinkEnterEvent event) {
        for (Map.Entry<String, String> entry : prStationsLinkID.entrySet()) {
            if (event.getLinkId().equals(Id.createLinkId(entry.getValue()))){
                // increase value of key by 1
                agentsPerPRStationLink.putIfAbsent(entry.getKey(),0.0);
                agentsPerPRStationLink.put(entry.getKey(),agentsPerPRStationLink.get(entry.getKey()) + 1.0);
            }
        }
    }

    public HashMap<String, String> getPrStationsLinkID() {
        return prStationsLinkID;
    }

    public void setPrStationsLinkID(HashMap<String, String> prStationsLinkID) {
        this.prStationsLinkID = prStationsLinkID;
    }

    public HashMap<String, Double> getAgentsPerPRStationLink() {
        return agentsPerPRStationLink;
    }

    public void setAgentsPerPRStationLink(HashMap<String, Double> agentsPerPRStationLink) {
        this.agentsPerPRStationLink = agentsPerPRStationLink;
    }
}