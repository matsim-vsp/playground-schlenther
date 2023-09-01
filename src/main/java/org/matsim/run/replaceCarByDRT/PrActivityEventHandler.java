package org.matsim.run.replaceCarByDRT;

import com.opencsv.CSVWriter;
import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * this class analyses the usage of P+R stations over the course of the day.
 * vehicles are assumed to be parked or picked up at P+R stations depending on whether the routing mode before/after
 * the P+R activity was TransportMode.car or not.
 */
class PrActivityEventHandler implements ActivityEndEventHandler, PersonDepartureEventHandler, ShutdownListener {

    private HashMap<PRStation, MutableInt> activitiesPerPRStation = new HashMap<>();

    private Map<PRStation, int[]> carsInPrStationPerMinute = new HashMap<>();

    private Map<Id<Person>, String> person2LastRoutingMode = new HashMap<>();

    private Map<PRStation, Set<Id<Person>>> station2Users = new HashMap<>();

    private Map<Id<Person>, Map.Entry<PRStation,Integer>> possibleRiders = new HashMap<>();

    private List<ActivityEndEvent> prActivityEndEvents = new LinkedList<>();


    public PrActivityEventHandler(URL url2PRStations) {
        Set<PRStation> prStations = PRStation.readPRStationFile(url2PRStations);
        for (PRStation station : prStations){
            this.activitiesPerPRStation.put(station, new MutableInt(0));
            this.carsInPrStationPerMinute.put(station, new int[36*60]);
            this.station2Users.put(station, new HashSet<>());
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {

        this.person2LastRoutingMode.put(event.getPersonId(), event.getRoutingMode());

        //checks if person could be a rider after a station
        if(this.possibleRiders.containsKey(event.getPersonId())){

            if(event.getRoutingMode().equals(TransportMode.ride)){
                //person uses ride after the station. All changes need to be undone.
                this.activitiesPerPRStation.get(possibleRiders.get(event.getPersonId()).getKey()).decrement();
                this.station2Users.remove(event.getPersonId());
                for (int i = possibleRiders.get(event.getPersonId()).getValue() - 1; i >= 0; i--) {
                    carsInPrStationPerMinute.get(possibleRiders.get(event.getPersonId()).getKey())[i] --;
                }

                this.possibleRiders.remove(event.getPersonId());

            } else {
                //person does not use ride after the station
                this.possibleRiders.remove(event.getPersonId());
            }
        }
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if(event.getActType().equals("P+R")){

            this.prActivityEndEvents.add(event);

            int endMinute = (int) Math.floor(event.getTime() / 60);

            //determine which station
            PRStation prStation = getPRStationWithCoord(event.getCoord());
            if(prStation == null){
                throw new IllegalArgumentException("could not find P+R station with coord = " + event.getCoord() + "!! \n The following event happens there: " + event);
            }

            // agents who ride to the station get skipped
            String lastRoutingMode = person2LastRoutingMode.get(event.getPersonId());
            if(!lastRoutingMode.equals(TransportMode.ride)){

                //determine whether vehicle is about to be parked or to be picked up
                if(lastRoutingMode.equals(TransportMode.car)){
                    //person arrives with a car, thus car is now about to get parked
                    for (int i = endMinute; i < 36*60; i++) {
                        carsInPrStationPerMinute.get(prStation)[i] ++;
                    }

                    //person now gets tracked that car is at this station
                    this.station2Users.get(prStation).add(event.getPersonId());

                } else {

                    //vehicle is picked up
                    if(this.station2Users.get(prStation).contains(event.getPersonId())) {
                        for (int i = endMinute; i < 36 * 60; i++) {
                            carsInPrStationPerMinute.get(prStation)[i]--;
                        }

                        //person now gets removed that car is not at this station anymore
                        this.station2Users.get(prStation).remove(event.getPersonId());

                    } else {
                        // Case 1: person was not tracked at this station before (even though we pick up the car)
                        // -> we are tracking a resident of the ban area and have to add the car for all previous time bins
                        for (int i = endMinute - 1; i >= 0; i--) {
                            carsInPrStationPerMinute.get(prStation)[i] ++;
                        }

                        //person now gets tracked that car is at this station
                        this.station2Users.get(prStation).add(event.getPersonId());

                        // Case 2: person uses mode ride after the station. Gets handled in first personDepartureEvent of the person after the station
                        this.possibleRiders.put(event.getPersonId(), Map.entry(prStation,endMinute));
                    }
                }
                this.activitiesPerPRStation.get(prStation).increment();
            }
        }
    }

    @Nullable
    private PRStation getPRStationWithCoord(Coord coord){
        for (PRStation prStation : activitiesPerPRStation.keySet()) {
            if(prStation.getCoord().equals(coord)) return prStation;
        }
        return null;
    }

    private void init(){
        this.person2LastRoutingMode.clear();
        this.prActivityEndEvents.clear();
        for (PRStation prStation : this.activitiesPerPRStation.keySet()) {
            this.activitiesPerPRStation.put(prStation, new MutableInt(0));
            this.carsInPrStationPerMinute.put(prStation, new int[36*60]);
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

    public List<ActivityEndEvent> getPrActivityEndEvents() {
        return prActivityEndEvents;
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {

        //write to CSV file
        String pathTotalActivitiesPerStation = event.getServices().getControlerIO().getOutputFilename("activitiesPerPRStation.tsv");
        String pathActivitiesPerMinute = event.getServices().getControlerIO().getOutputFilename("carsInPrStationPerMinute.tsv");
        String prActivitiesFile = event.getServices().getControlerIO().getOutputFilename("PR_activities.tsv");

        try {
            writeAgentsPerPRStation(pathTotalActivitiesPerStation);
            writeCarsInPrStationPerMinute(pathActivitiesPerMinute);
            writePRActivitiesFile(prActivitiesFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void writePRActivitiesFile(String prActivitiesFile) throws IOException {
        CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(prActivitiesFile)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
        writer.writeNext(new String[]{"time","person","linkId","x","y"});
        for (ActivityEndEvent prActivityEndEvent : this.prActivityEndEvents) {
            writer.writeNext(new String[]{
                    "" + prActivityEndEvent.getTime(),
                    prActivityEndEvent.getPersonId().toString(),
                    prActivityEndEvent.getLinkId().toString(),
                    "" + prActivityEndEvent.getCoord().getX(),
                    "" + prActivityEndEvent.getCoord().getY()
            });
        }
        writer.close();
    }

    private void writeAgentsPerPRStation(String outputFileName) throws IOException {
        CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
        writer.writeNext(new String[]{"PRStation","Agents","x","y"});

        for (Map.Entry<PRStation, MutableInt> entry : this.activitiesPerPRStation.entrySet()) {
            PRStation station = entry.getKey();
            MutableInt agentsPerPRStation = entry.getValue();


            writer.writeNext(new String[]{station.getName(),String.valueOf(agentsPerPRStation),String.valueOf(station.getCoord().getX()),String.valueOf(station.getCoord().getY())});
        }
        writer.close();
    }

    private void writeCarsInPrStationPerMinute(String outputFileName) throws IOException {
        CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");

        List<String> header = new ArrayList<String>();
        header.add("PRStation");
        for (int i = 0; i < 36; i++){
            for(int j = 0; j < 60; j++) {
                header.add(i + ":" + j);
            }
        }
        writer.writeNext(header.toArray(new String[0]));
        for (Map.Entry<PRStation, int[]> entry : this.carsInPrStationPerMinute.entrySet()) {
            PRStation station = entry.getKey();
            int[] agentsPerMinute = entry.getValue();
            String[] agents = Arrays.toString(agentsPerMinute).split("[\\[\\]]")[1].split(", ");
            List<String> agentsList = new ArrayList<String>(Arrays.asList(agents));
            agentsList.add(0, station.getName());

            writer.writeNext(agentsList.toArray(new String[0]));
        }
        writer.close();
    }
}