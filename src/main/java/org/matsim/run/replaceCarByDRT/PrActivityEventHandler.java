package org.matsim.run.replaceCarByDRT;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class PrActivityEventHandler implements ActivityStartEventHandler {

    private static final Logger log = Logger.getLogger(PrActivityEventHandler.class);

    //key = name, value = coord
    private HashMap<PRStation, Integer> agentsPerPRStation = new HashMap<PRStation, Integer>();

    private Map<PRStation, int[]> prStartsPerHour = new HashMap<>();

    public PrActivityEventHandler(Set<PRStation> prStations) {
        for (PRStation station : prStations){
            this.agentsPerPRStation.put(station, 0);
            this.prStartsPerHour.put(station, new int[36]);
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().equals("P+R")) {

            int hour = (int) Math.floor(event.getTime() / 3600); //TODO check if this works and whether we can improve (i.e. don't cast)

            PRStation prStation = getPRStationWithCoord(event.getCoord());
            if(prStation == null){
                throw new IllegalArgumentException("could not find P+R station with coord = " + event.getCoord() + "!! \n The following event happens there: " + event);
            } else {
                agentsPerPRStation.put(prStation, agentsPerPRStation.get(prStation) + 1);
                prStartsPerHour.get(prStation)[hour] ++;
            }
        }
    }

    @Nullable
    private PRStation getPRStationWithCoord(Coord coord){
        PRStation result = null;
        for (PRStation prStation : agentsPerPRStation.keySet()) {
            if(prStation.coord.equals(coord)) return prStation;
        }
        return null;
    }

    private void init(){
        for (PRStation prStation : agentsPerPRStation.keySet()) {
            agentsPerPRStation.put(prStation, 0);
        }
    }

    @Override
    public void reset(int iteration) {
        init();
    }

    public HashMap<PRStation, Integer> getAgentsPerPRStation() {
        return agentsPerPRStation;
    }

    public Map<PRStation, int[]> getPrStartsPerHour() {
        return prStartsPerHour;
    }
}
