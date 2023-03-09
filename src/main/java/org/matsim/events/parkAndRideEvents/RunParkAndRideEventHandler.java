package org.matsim.events.parkAndRideEvents;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.io.*;
import java.util.*;

/**
 * This class contains a main method to call the
 * example event handlers MyEventHandler1-3.
 *
 * @author dgrether
 */

public class RunParkAndRideEventHandler {

    public static void main(String[] args) {

        String inputFile = "scenarios/output/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_events.xml.gz";

        // read CSV file
        String csvFilePath = "scenarios/berlin/replaceCarByDRT/noModeChoice/2023-01-17-pr-stations.csv";
        HashMap<String, String> prStationsLinkID = new HashMap<String, String>();
        try{
            BufferedReader lineRead = new BufferedReader (new FileReader(csvFilePath));
            CSVParser records = CSVParser.parse(lineRead, CSVFormat.EXCEL.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
            for (CSVRecord record : records){
                prStationsLinkID.put(record.get(0),record.get(1));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //create an event object
        EventsManager events = EventsUtils.createEventsManager();

        //create the handler and add it + sets LinkOfInterest to current PRLink
        parkAndRideEventHandler handler1 = new parkAndRideEventHandler();
        handler1.setPrStationsLinkID(prStationsLinkID);
        events.addHandler(handler1);

        //create the reader and read the file
        events.initProcessing();
        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputFile);
        events.finishProcessing();

        System.out.println(handler1.getAgentsPerPRStationLink());
    }
}
