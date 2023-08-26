package org.matsim.run.replaceCarByDRT;

import com.opencsv.CSVWriter;
import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RunPRActivityEventHandler {

    private final String runDirectory;
    private final String runId;
    private final URL pr_stations;

    public RunPRActivityEventHandler(String runDirectory, String runId, URL prStations) {
        this.runDirectory = runDirectory;
        this.runId = runId;
        this.pr_stations = prStations;
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            String runDirectory = "scenarios/output/runs-2023-08-11/stationChoice-closestToOutside/";
            //Cluster: scenarios/output/runs-2023-06-02/extraPtPlan-true/drtStopBased-true/massConservation-true/
            String runId = "stationChoice-closestToOutside";
            URL tsvFilePath = IOUtils.resolveFileOrResource("scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-07-27-pr-stations.tsv");

            RunPRActivityEventHandler prActivities = new RunPRActivityEventHandler(runDirectory, runId, tsvFilePath);
            prActivities.run();
        } else {
            String runDirectory = args[0];
            String runId = args[1];
            URL tsvFilePath = IOUtils.resolveFileOrResource(args[2]);

            RunPRActivityEventHandler prActivities = new RunPRActivityEventHandler(runDirectory, runId, tsvFilePath);
            prActivities.run();
        }
    }

    public void run(){
        String inputFile = runDirectory + runId + ".output_events.xml.gz";

        //create an event object
        EventsManager events = EventsUtils.createEventsManager();

        //create the handler and add it + sets LinkOfInterest to current PRLink
        PrActivityEventHandler handler = new PrActivityEventHandler(pr_stations);
        events.addHandler(handler);

        //create the reader and read the file
        events.initProcessing();
        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputFile);
        events.finishProcessing();

        //write to CSV file
        String pathTotalActivitiesPerStation = inputFile.substring(0, inputFile.lastIndexOf(".xml")) + "_activitiesPerPRStation.tsv";
        String pathActivitiesPerMinute = inputFile.substring(0, inputFile.lastIndexOf(".xml")) + "_carsInPrStationPerMinute.tsv";
        String prActivitiesFile = inputFile.substring(0, inputFile.lastIndexOf(".xml")) + "_PR_activites.tsv";

        try {
            writeAgentsPerPRStation(handler.getActivitiesPerPRStation(), pathTotalActivitiesPerStation);
            writeCarsInPrStationPerMinute(handler.getCarsInPrStationPerMinute(), pathActivitiesPerMinute);
            writePRActivitiesFile(handler, prActivitiesFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void writePRActivitiesFile(PrActivityEventHandler handler, String prActivitiesFile) throws IOException {
        CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(prActivitiesFile)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
        writer.writeNext(new String[]{"time","person","linkId","x","y"});
        for (ActivityEndEvent prActivityEndEvent : handler.getPrActivityEndEvents()) {
            writer.writeNext(new String[]{
                    "" + prActivityEndEvent.getTime(),
                    prActivityEndEvent.getPersonId().toString(),
                    prActivityEndEvent.getLinkId().toString(),
                    "" + prActivityEndEvent.getCoord().getX(),
                    "" + prActivityEndEvent.getCoord().getY()
            });
        }
    }

    private static void writeAgentsPerPRStation(Map<PRStation, MutableInt> prActivities, String outputFileName) throws IOException {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
            writer.writeNext(new String[]{"PRStation","Agents","x","y"});

            for (Map.Entry<PRStation, MutableInt> entry : prActivities.entrySet()) {
                PRStation station = entry.getKey();
                MutableInt agentsPerPRStation = entry.getValue();


                writer.writeNext(new String[]{station.getName(),String.valueOf(agentsPerPRStation),String.valueOf(station.coord.getX()),String.valueOf(station.coord.getY())});
            }
            writer.close();


    }

    private static void writeCarsInPrStationPerMinute(Map<PRStation, int[]> prActivities, String outputFileName) throws IOException {
        try{
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");

            List<String> header = new ArrayList<String>();
            header.add("PRStation");
            for (int i = 0; i < 36; i++){
                for(int j = 0; j < 60; j++) {
                    header.add(i + ":" + j);
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
