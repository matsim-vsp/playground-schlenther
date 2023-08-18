package org.matsim.run.replaceCarByDRT;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * this class analyses the usage of P+R stations over the course of the day.
 * vehicles are assumed to be parked or picked up at P+R stations depending on whether the routing mode before/after
 * the P+R activity was TransportMode.car or not.
 */
class PrActivityEventHandler implements /*ActivityStartEventHandler,*/ ActivityEndEventHandler, PersonDepartureEventHandler {

    private static final Logger log = Logger.getLogger(PrActivityEventHandler.class);

    //key = name, value = coord
    private HashMap<PRStation, MutableInt> activitiesPerPRStation = new HashMap<>();

//    private Map<PRStation, int[]> prStartsPerHour = new HashMap<>();

//    private Map<PRStation, int[]> prActivitiesPerMinute = new HashMap<>();

    private Map<PRStation, int[]> carsInPrStationPerMinute = new HashMap<>();

    private Map<Id<Person>, String> person2LastRoutingMode = new HashMap<>();

    private Map<PRStation, Set<Id<Person>>> station2Users = new HashMap<>();


    public PrActivityEventHandler(Set<PRStation> prStations) {
        for (PRStation station : prStations){
            this.activitiesPerPRStation.put(station, new MutableInt(0));
            this.carsInPrStationPerMinute.put(station, new int[36*60]);
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        this.person2LastRoutingMode.put(event.getPersonId(), event.getRoutingMode());
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if(event.getActType().equals("P+R")){

            int endMinute = (int) Math.floor(event.getTime() / 60);

            //determine which station
            PRStation prStation = getPRStationWithCoord(event.getCoord());
            if(prStation == null){
                throw new IllegalArgumentException("could not find P+R station with coord = " + event.getCoord() + "!! \n The following event happens there: " + event);
            } else {
                    //determine whether vehicle is about to be parked or to be picked up
                    if(person2LastRoutingMode.get(event.getPersonId()).equals(TransportMode.car)){
                        //person arrives with a car, thus car is now about to get parked
                        for (int i = endMinute; i < 36*60; i++) {
                            carsInPrStationPerMinute.get(prStation)[i] ++;
                        }
                    } else {
                        //vehicle is picked up
                        if(this.station2Users.get(prStation).contains(event.getPersonId())) {
                            for (int i = endMinute; i < 36 * 60; i++) {
                                carsInPrStationPerMinute.get(prStation)[i]--;
                            }
                        } else {
                            //person was not tracked at this station before (even though we pick up the car)
                            // -> we are tracking a resident of the ban area and have to add the car for all previous time bins
                            for (int i = endMinute - 1; i >= 0; i--) {
                                carsInPrStationPerMinute.get(prStation)[i] ++;
                            }
                        }
                    }
                }
            this.station2Users.get(prStation).add(event.getPersonId());
            this.activitiesPerPRStation.get(prStation).increment();
        }
    }

    @Nullable
    private PRStation getPRStationWithCoord(Coord coord){
        for (PRStation prStation : activitiesPerPRStation.keySet()) {
            if(prStation.coord.equals(coord)) return prStation;
        }
        return null;
    }

    private void init(){
        this.person2LastRoutingMode.clear();
        for (PRStation prStation : this.activitiesPerPRStation.keySet()) {
            this.activitiesPerPRStation.put(prStation, new MutableInt(0));
            this.station2Users.put(prStation, new HashSet<>());
        }
    }

    @Override
    public void reset(int iteration) {
        init();
    }

    public HashMap<PRStation, MutableInt> getActivitiesPerPRStation() {
        return activitiesPerPRStation;
    }

    public Map<PRStation, int[]> getCarsInPrStationPerMinute() {
        return carsInPrStationPerMinute;
    }
}
