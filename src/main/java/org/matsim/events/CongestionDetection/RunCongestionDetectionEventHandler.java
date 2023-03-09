package org.matsim.events.CongestionDetection;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.events.parkAndRideEvents.parkAndRideEventHandler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class RunCongestionDetectionEventHandler {

    public static void main(String[] args) {
        String inputFile = "scenarios/output/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_events.xml.gz";

        var network = NetworkUtils.readNetwork("scenarios/output/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_network.xml.gz");

        // read CSV file
        String csvFilePath = "scenarios/berlin/replaceCarByDRT/noModeChoice/2023-01-17-pr-stations.csv";
        HashMap<String, String> prStationsMap = new HashMap<String, String>();
        try{
            BufferedReader lineRead = new BufferedReader (new FileReader(csvFilePath));
            CSVParser records = CSVParser.parse(lineRead, CSVFormat.EXCEL.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
            for (CSVRecord record : records){
                prStationsMap.put(record.get(0),record.get(1));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //create an event object
        EventsManager events = EventsUtils.createEventsManager();

        //create the handler and add it
        CongestionDetectionEventHandler handler1 = new CongestionDetectionEventHandler(network);
        handler1.setPrStationsMap(prStationsMap);
        events.addHandler(handler1);

        //create the reader and read the file
        events.initProcessing();
        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputFile);
        events.finishProcessing();

        System.out.println("Events file read!");
    }


}
