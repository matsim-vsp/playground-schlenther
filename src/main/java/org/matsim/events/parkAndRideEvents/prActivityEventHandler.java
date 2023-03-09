package org.matsim.events.parkAndRideEvents;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;

import java.util.HashMap;
import java.util.Map;

public class prActivityEventHandler implements ActivityStartEventHandler, ActivityEndEventHandler {

    private HashMap<String, String> prStationsLinkID = new HashMap<String, String>();
    private HashMap<String, String> prStationsX = new HashMap<String, String>();
    private HashMap<String, String> prStationsY = new HashMap<String, String>();

    private HashMap<String, Integer> agentsPerPRStation = new HashMap<String, Integer>();

    @Override
    public void handleEvent(ActivityEndEvent event) {
       for (Map.Entry<String, String> entry : prStationsX.entrySet()) {
            if (event.getActType().equals("P+R")) {
                if (Double.toString(event.getCoord().getX()).equals(entry.getValue())) {
                    // increase value of key by 1
                    agentsPerPRStation.putIfAbsent(entry.getKey(), 0);
                    agentsPerPRStation.put(entry.getKey(), agentsPerPRStation.get(entry.getKey()) + 1);
                }
            }
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        for (Map.Entry<String, String> entry : prStationsX.entrySet()) {
            if (event.getActType().equals("P+R")) {
                if (String.valueOf(event.getCoord().getX()) == entry.getValue()) {
                    // increase value of key by 1
                    agentsPerPRStation.putIfAbsent(entry.getKey(), 0);
                    agentsPerPRStation.put(entry.getKey(), agentsPerPRStation.get(entry.getKey()) + 1);
                }
            }
        }
    }

    public HashMap<String, String> getPrStationsLinkID() {
        return prStationsLinkID;
    }

    public void setPrStationsLinkID(HashMap<String, String> prStationsLinkID) {
        this.prStationsLinkID = prStationsLinkID;
    }

    public HashMap<String, Integer> getAgentsPerPRStation() {
        return agentsPerPRStation;
    }

    public void setAgentsPerPRStation(HashMap<String, Integer> agentsPerPRStation) {
        this.agentsPerPRStation = agentsPerPRStation;
    }

    public HashMap<String, String> getPrStationsX() {
        return prStationsX;
    }

    public void setPrStationsX(HashMap<String, String> prStationsX) {
        this.prStationsX = prStationsX;
    }

    public HashMap<String, String> getPrStationsY() {
        return prStationsY;
    }

    public void setPrStationsY(HashMap<String, String> prStationsY) {
        this.prStationsY = prStationsY;
    }
}
