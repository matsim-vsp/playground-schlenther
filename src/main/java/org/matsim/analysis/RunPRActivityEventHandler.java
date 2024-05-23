package org.matsim.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.net.URL;

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
            handler.writeAgentsPerPRStation(pathTotalActivitiesPerStation);
            handler.writeCarsInPrStationPerMinute(pathActivitiesPerMinute);
            handler.writePRActivitiesFile(prActivitiesFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
