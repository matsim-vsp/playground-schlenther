package org.matsim.run.replaceCarByDRT;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.replaceCarByDRT.PrActivityEventHandler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public class RunPRActivityEventHandler {

    public static void main(String[] args) {

        String inputFile = "scenarios/output/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_events.xml.gz";

        // read CSV file
        String csvFilePath = "scenarios/berlin/replaceCarByDRT/noModeChoice/2023-01-17-pr-stations-2.csv";
        Set<PRStation> prStations = ReplaceCarByDRT.readPRStationFile(IOUtils.resolveFileOrResource(csvFilePath));

        //create an event object
        EventsManager events = EventsUtils.createEventsManager();

        //create the handler and add it + sets LinkOfInterest to current PRLink
        PrActivityEventHandler handler1 = new PrActivityEventHandler(prStations);
        events.addHandler(handler1);

        //create the reader and read the file
        events.initProcessing();
        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputFile);
        events.finishProcessing();

        System.out.println(handler1.getAgentsPerPRStation());
    }

}
