package org.matsim.run.replaceCarByDRT;

import com.opencsv.CSVWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RunPRActivityEventHandler {

    public static void main(String[] args) {

       // String inputFile = "scenarios/output/berlin-v5.5-sample/inside-allow-0.5-1506vehicles-8seats.output_events.xml.gz"; //TEST-INPUT
       String inputFile = "scenarios/output/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_events.xml.gz";

        // read CSV file
        String tsvFilePath = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-01-17-pr-stations.tsv";
        Set<PRStation> prStations = ReplaceCarByDRT.readPRStationFile(IOUtils.resolveFileOrResource(tsvFilePath));

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

        //write to CSV file
        String outputFileName1 = "scenarios/output/inside-allow-0.5-1506vehicles-8seats/analysis/PRStations/prStartsPerHour.tsv";
        String outputFileName2 = "scenarios/output/inside-allow-0.5-1506vehicles-8seats/analysis/PRStations/prActivitiesPerMinute.tsv";
        String outputFileName3= "scenarios/output/inside-allow-0.5-1506vehicles-8seats/analysis/PRStations/agentsPerPRStation.tsv";
        agentsPerPRStation2CSV(handler1.getAgentsPerPRStation(),outputFileName3);
        prStartsPerHour2CSV(handler1.getPrStartsPerHour(),outputFileName1);
        prActivitiesPerMinute2CSV(handler1.getPrActivitiesPerMinute(), outputFileName2);
    }

    private static void agentsPerPRStation2CSV(Map<PRStation, Integer> prActivities, String outputFileName){
        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
            writer.writeNext(new String[]{"PRStation","Agents"});

            for (Map.Entry<PRStation, Integer> entry : prActivities.entrySet()) {
                PRStation station = entry.getKey();
                Integer agentsPerPRStation = entry.getValue();

                writer.writeNext(new String[]{station.getName(),String.valueOf(agentsPerPRStation)});
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private static void prStartsPerHour2CSV(Map<PRStation, int[]> prActivities, String outputFileName){
        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");

            List<String> header = new ArrayList<String>();
            header.add("PRStation");
            for (int i = 0; i < 36; i++){
                header.add(i + ":00 - " + i + ":59");
            }
            writer.writeNext(header.toArray(new String[0]));

            for (Map.Entry<PRStation, int[]> entry : prActivities.entrySet()) {
                PRStation station = entry.getKey();
                int[] agentsPerHour = entry.getValue();
                String[] agents = Arrays.toString(agentsPerHour).split("[\\[\\]]")[1].split(", ");
                List<String> agentsList = new ArrayList<String>(Arrays.asList(agents));
                agentsList.add(0, station.getName());

                writer.writeNext(agentsList.toArray(new String[0]));
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void prActivitiesPerMinute2CSV(Map<PRStation, int[]> prActivities, String outputFileName){
        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");

            List<String> header = new ArrayList<String>();
            header.add("PRStation");
            for (int i = 0; i < 36; i++){
                for(int j = 0; j < 60; j++) {
                    header.add(i + ":" + j + ":00 - " + i + ":" + j + ":59");
                }
            }
            writer.writeNext(header.toArray(new String[0]));

            for (Map.Entry<PRStation, int[]> entry : prActivities.entrySet()) {
                PRStation station = entry.getKey();
                int[] agentsPerMinute = entry.getValue();
                String[] agents = Arrays.toString(agentsPerMinute).split("[\\[\\]]")[1].split(", ");
                List<String> agentsList = new ArrayList<String>(Arrays.asList(agents));
                agentsList.add(0, station.getName());

                writer.writeNext(agentsList.toArray(new String[0]));
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
