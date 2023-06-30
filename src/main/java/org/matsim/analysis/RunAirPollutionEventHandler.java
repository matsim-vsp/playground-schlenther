package org.matsim.analysis;

import com.opencsv.CSVWriter;
import org.matsim.analysis.emissions.EmissionsOnLinkHandler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.EmissionUtils;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsPerPersonColdEventHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class RunAirPollutionEventHandler {

    public static void main(String[] args) {
        String inputFile = "scenarios/output/runs-2023-06-13/finalRun-10pct/massConservation-true/analysis/airPollution/final-10pct-7503vehicles-8seats.emission.events.offline.xml.gz";

        //create an event object
        EventsManager eventsManager = EventsUtils.createEventsManager();

        //create the handler and add it
        EmissionsOnLinkHandler emissionsHandler = new EmissionsOnLinkHandler();
        eventsManager.addHandler(emissionsHandler);

        //create the reader and read the file
        eventsManager.initProcessing();
        EmissionEventsReader eventsReader = new EmissionEventsReader(eventsManager);
        eventsReader.readFile(inputFile);
        eventsManager.finishProcessing();

        //write to CSV file
        String outputFileName1 = inputFile.substring(0, inputFile.lastIndexOf(".xml")) + "_airPollution_Costs.tsv";
        EmissionsPerLink2CSV(emissionsHandler.getLink2pollutants(),outputFileName1);

    }

    private static void EmissionsPerLink2CSV(Map<Id<Link>, Map<Pollutant, Double>> DTVPerLink, String outputFileName){
        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
            writer.writeNext(new String[]{"link","pollutant","emissions"});

            for (Map.Entry<Id<Link>, Map<Pollutant, Double>> entry : DTVPerLink.entrySet()) {
                Id<Link> linkId = entry.getKey();
                Map<Pollutant, Double> link_emissions_map = entry.getValue();

                for(Map.Entry<Pollutant, Double> entry2 : link_emissions_map.entrySet()) {
                    Pollutant pollutant = entry2.getKey();
                    Double link_emissions_by_pollutant = entry2.getValue();

                    writer.writeNext(new String[]{String.valueOf(linkId), String.valueOf(pollutant), String.valueOf(link_emissions_by_pollutant)});
                }
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
