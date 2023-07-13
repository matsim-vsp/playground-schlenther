package org.matsim.run.replaceCarByDRT;

import com.opencsv.CSVWriter;
import org.apache.log4j.Logger;
import org.matsim.analysis.RunTripsPreparation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RunPRActivityEventHandler {

    private final String runDirectory;
    private final String runId;
    private final String pr_stations;


    private static final Logger log = Logger.getLogger(RunTripsPreparation.class);


    public RunPRActivityEventHandler(String runDirectory, String runId, String prStations) {
        this.runDirectory = runDirectory;
        this.runId = runId;
        this.pr_stations = prStations;

    }


    public static void main(String[] args) {

        if (args.length == 0) {
            String runDirectory = "scenarios/output/runs-2023-05-26/extraPtPlan-false/drtStopBased-false/massConservation-true/";
            //Cluster: scenarios/output/runs-2023-06-02/extraPtPlan-true/drtStopBased-true/massConservation-true/
            String runId = "massConservation-1506vehicles-8seats";
            String tsvFilePath = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";

            RunPRActivityEventHandler prActivities = new RunPRActivityEventHandler(runDirectory, runId, tsvFilePath);
            prActivities.run();
        } else {
            String runDirectory = args[0];
            String runId = args[1];
            String tsvFilePath = args[2];

            RunPRActivityEventHandler prActivities = new RunPRActivityEventHandler(runDirectory, runId, tsvFilePath);
            prActivities.run();
        }
    }

    public void run(){
        String inputFile = runDirectory + runId + ".output_events.xml.gz";
        String inputNetwork = runDirectory + runId + ".output_network.xml.gz";

        Network network = NetworkUtils.readNetwork(inputNetwork);

        // read CSV file
        Set<PRStation> prStationsSet = ReplaceCarByDRT.readPRStationFile(IOUtils.resolveFileOrResource(pr_stations));

        //create an event object
        EventsManager events = EventsUtils.createEventsManager();

        //create the handler and add it + sets LinkOfInterest to current PRLink
        PrActivityEventHandler handler1 = new PrActivityEventHandler(prStationsSet);
        events.addHandler(handler1);

        //create the reader and read the file
        events.initProcessing();
        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputFile);
        events.finishProcessing();

        //write to CSV file
        String outputFileName1 = inputFile.substring(0, inputFile.lastIndexOf(".xml")) + "_prStartsPerHour.tsv";
        String outputFileName2 = inputFile.substring(0, inputFile.lastIndexOf(".xml")) + "_prActivitiesPerMinute.tsv";
        String outputFileName3 = inputFile.substring(0, inputFile.lastIndexOf(".xml")) + "_agentsPerPRStation.tsv";
        agentsPerPRStation2CSV(handler1.getAgentsPerPRStation(), network, outputFileName3);
        prStartsPerHour2CSV(handler1.getPrStartsPerHour(),outputFileName1);
        prActivitiesPerMinute2CSV(handler1.getPrActivitiesPerMinute(), outputFileName2);
    }

    private static void agentsPerPRStation2CSV(Map<PRStation, Integer> prActivities, Network network, String outputFileName){
        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
            writer.writeNext(new String[]{"PRStation","Agents","x","y","AgentsByEdgeCapacity"});

            for (Map.Entry<PRStation, Integer> entry : prActivities.entrySet()) {
                PRStation station = entry.getKey();
                Integer agentsPerPRStation = entry.getValue();
                Link link = network.getLinks().get(station.linkId);
                Double edgeCapacity = link.getCapacity();
                Double agentsByEdgeCapacity = Double.valueOf(agentsPerPRStation) / (edgeCapacity * 36);

                writer.writeNext(new String[]{station.getName(),String.valueOf(agentsPerPRStation),String.valueOf(station.coord.getX()),String.valueOf(station.coord.getY()),String.valueOf(agentsByEdgeCapacity)});
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
