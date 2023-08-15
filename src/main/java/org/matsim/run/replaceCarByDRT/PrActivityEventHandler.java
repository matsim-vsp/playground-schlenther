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
    private HashMap<PRStation, Integer> activitiesPerPRStation = new HashMap<>();

    private Map<PRStation, int[]> prStartsPerHour = new HashMap<>();

    private Map<PRStation, int[]> prActivitiesPerMinute = new HashMap<>();

    private Map<PRStation, int[]> carsInPrStationPerMinute = new HashMap<>();

    private Map<Id<Person>, PRStation> personPerPrStation = new HashMap<>();
    private Map<Id<Person>,Double> startTime = new HashMap<>();

    public PrActivityEventHandler(Set<PRStation> prStations) {
        for (PRStation station : prStations){
            this.activitiesPerPRStation.put(station, 0);
            this.prStartsPerHour.put(station, new int[36]);
            this.prActivitiesPerMinute.put(station, new int[36*60]);
            this.carsInPrStationPerMinute.put(station, new int[36*60]);
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
//        if(event.getPersonId().toString().equals("404930501")){
            if (event.getActType().equals("P+R")) {

                int hour = (int) Math.floor(event.getTime() / 3600); //TODO check if this works and whether we can improve (i.e. don't cast)
                int minute = (int) Math.floor(event.getTime() / 60);

                PRStation prStation = getPRStationWithCoord(event.getCoord());

                if(prStation == null){
                    throw new IllegalArgumentException("could not find P+R station with coord = " + event.getCoord() + "!! \n The following event happens there: " + event);
                } else {
                    activitiesPerPRStation.put(prStation, activitiesPerPRStation.get(prStation) + 1);
                    for (int i = minute; i < 36*60; i++){
                        prActivitiesPerMinute.get(prStation)[i] ++;
                    }
                    prStartsPerHour.get(prStation)[hour] ++;
                }

                Map<Id<Person>, PRStation> actualPersonInPrStation = new HashMap<>();
                actualPersonInPrStation.put(event.getPersonId(), prStation);

                if(!personPerPrStation.entrySet().containsAll(actualPersonInPrStation.entrySet())){
                    personPerPrStation.put(event.getPersonId(), prStation);
                    startTime.put(event.getPersonId(),event.getTime());
                    for (int i = minute; i < 36*60; i++) {
                        carsInPrStationPerMinute.get(prStation)[i] ++;
                    }
                }

//            }
        }



    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
//        if(event.getPersonId().toString().equals("404930501")){
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

                Map<Id<Person>, PRStation> actualPersonInPrStation = new HashMap<>();
                actualPersonInPrStation.put(event.getPersonId(), prStation);

                Double firstTime = startTime.get(event.getPersonId());
                Double endTime = event.getTime();

                if(personPerPrStation.entrySet().containsAll(actualPersonInPrStation.entrySet())){
                    // TODO: Test all this
                    if(Math.abs(endTime - firstTime) >= 6 * 60) {
                        personPerPrStation.remove(event.getPersonId(), prStation);
                        for (int i = endMinute; i < 36*60; i++) {
                            carsInPrStationPerMinute.get(prStation)[i] --;
                        }
                    }
                } else {
                    log.warn("Agent leaves P+R Station without having entered it. This should not happen.");
                }

            }

//        }

    }

    @Nullable
    private PRStation getPRStationWithCoord(Coord coord){
        PRStation result = null;
        for (PRStation prStation : activitiesPerPRStation.keySet()) {
            if(prStation.coord.equals(coord)) return prStation;
        }
        return null;
    }

    private void init(){
        for (PRStation prStation : activitiesPerPRStation.keySet()) {
            activitiesPerPRStation.put(prStation, 0);
        }
    }

    @Override
    public void reset(int iteration) {
        init();
    }

    public HashMap<PRStation, Integer> getActivitiesPerPRStation() {
        return activitiesPerPRStation;
    }

    public Map<PRStation, int[]> getPrStartsPerHour() {
        return prStartsPerHour;
    }

    public Map<PRStation, int[]> getPrActivitiesPerMinute() {
        return prActivitiesPerMinute;
    }

    public Map<PRStation, int[]> getCarsInPrStationPerMinute() {
        return carsInPrStationPerMinute;
    }

}
