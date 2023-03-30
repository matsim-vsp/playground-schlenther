package org.matsim.run.replaceCarByDRT;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class PrActivityEventHandler implements ActivityStartEventHandler, ActivityEndEventHandler {

    private static final Logger log = Logger.getLogger(PrActivityEventHandler.class);

    //key = name, value = coord
    private HashMap<PRStation, Integer> agentsPerPRStation = new HashMap<>();

    private Map<PRStation, int[]> prStartsPerHour = new HashMap<>();

    private Map<PRStation, int[]> prActivitiesPerMinute = new HashMap<>();

    private Map<Id<Person>,Double> startTime = new HashMap<>();

    public PrActivityEventHandler(Set<PRStation> prStations) {
        for (PRStation station : prStations){
            this.agentsPerPRStation.put(station, 0);
            this.prStartsPerHour.put(station, new int[36]);
            this.prActivitiesPerMinute.put(station, new int[36*60]);
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().equals("P+R")) {

            int hour = (int) Math.floor(event.getTime() / 3600); //TODO check if this works and whether we can improve (i.e. don't cast)
            int minute = (int) Math.floor(event.getTime() / 60);

            PRStation prStation = getPRStationWithCoord(event.getCoord());
            if(prStation == null){
                throw new IllegalArgumentException("could not find P+R station with coord = " + event.getCoord() + "!! \n The following event happens there: " + event);
            } else {
                agentsPerPRStation.put(prStation, agentsPerPRStation.get(prStation) + 1);

                for (int i = minute; i < 36*60; i++){
                    prActivitiesPerMinute.get(prStation)[i] ++;
                }

                prStartsPerHour.get(prStation)[hour] ++;
            }
        }


    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if(event.getActType().equals("P+R")){
            int endHour = (int) Math.floor(event.getTime() / 3600);
            int endMinute = (int) Math.floor(event.getTime() / 60);

            PRStation prStation = getPRStationWithCoord(event.getCoord());
            if(prStation == null){
                throw new IllegalArgumentException("could not find P+R station with coord = " + event.getCoord() + "!! \n The following event happens there: " + event);
            } else {
                for (int i = endMinute+1; i < 36*60; i++){
                prActivitiesPerMinute.get(prStation)[i] --;
                }
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

    public Map<PRStation, int[]> getPrActivitiesPerMinute() {
        return prActivitiesPerMinute;
    }

}
