package org.matsim.run.replaceCarByDRT;

import com.opencsv.CSVWriter;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.vehicles.Vehicles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RunTrafficVolumeEventHandler {

    private final String runDirectory;
    private final String runId;
    private final String inner_city_shp;
    private final String berlin_shp;

    private static final Logger log = Logger.getLogger(RunTripsPreparation.class);

    public RunTrafficVolumeEventHandler(String runDirectory, String runId, String inner_city_shp, String berlin_shp){
        this.runDirectory = runDirectory;
        this.runId = runId;
        this.inner_city_shp = inner_city_shp;
        this.berlin_shp = berlin_shp;
    }

    public static void main(String[] args) {
        if(args.length == 0) {
            String runDirectory = "scenarios/output/runs-2023-05-26/extraPtPlan-false/drtStopBased-false/massConservation-true/";
            String runId = "massConservation-1506vehicles-8seats";
            String inner_city_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            String berlin_shp = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";

            RunTrafficVolumeEventHandler trafficVolumes = new RunTrafficVolumeEventHandler(runDirectory, runId, inner_city_shp, berlin_shp);
            trafficVolumes.run();
        } else {
            String runDirectory = args[0];
            String runId = args[1];
            String inner_city_shp = args[2];
            String berlin_shp = args[3];

            RunTrafficVolumeEventHandler trafficVolumes = new RunTrafficVolumeEventHandler(runDirectory, runId, inner_city_shp, berlin_shp);
            trafficVolumes.run();

        }
    }

    public void run() {
        String inputFile = runDirectory + runId + ".output_events.xml.gz";
        String inputNetwork = runDirectory + runId + ".output_network.xml.gz";

        Network network = NetworkUtils.readNetwork(inputNetwork);

        List<PreparedGeometry> innerCity = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(inner_city_shp));
        List<PreparedGeometry> berlin = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(berlin_shp));

        //create an event object
        EventsManager events = EventsUtils.createEventsManager();

        //create the handler and add it + sets LinkOfInterest to current PRLink
        TrafficVolumeEventHandler handler1 = new TrafficVolumeEventHandler(network);
        events.addHandler(handler1);

        //create the reader and read the file
        events.initProcessing();
        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(inputFile);
        events.finishProcessing();

        //write to CSV file
        String outputFileName1 = inputFile.substring(0, inputFile.lastIndexOf(".xml")) + "_dailyTrafficVolume_vehicles.tsv";
        String mileageOutputFileName = inputFile.substring(0, inputFile.lastIndexOf(".xml")) + "_dailyMileage_vehicles.tsv";
        DTVPerLink2CSV(handler1.getDTVPerLink_Total(),network, innerCity, berlin, outputFileName1);
        mileagePerLink2CSV(handler1.getDTVPerLink_Total(), network, innerCity, berlin, mileageOutputFileName);
    }

    private static void DTVPerLink2CSV(Map<Id<Link>, Integer> DTVPerLink, Network network, List<PreparedGeometry> innerCity_shp, List<PreparedGeometry> berlin_shp, String outputFileName){
        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
            writer.writeNext(new String[]{"link","agents"});

            for (Map.Entry<Id<Link>, Integer> entry : DTVPerLink.entrySet()) {
                Id<Link> linkId = entry.getKey();
                Integer link_DTV = entry.getValue();
                writer.writeNext(new String[]{String.valueOf(linkId), String.valueOf(link_DTV)});

            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void mileagePerLink2CSV(Map<Id<Link>, Integer> DTVPerLink, Network network, List<PreparedGeometry> innerCity_shp, List<PreparedGeometry> berlin_shp, String outputFileName){
        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
            writer.writeNext(new String[]{"link","mileage"});

            for (Map.Entry<Id<Link>, Integer> entry : DTVPerLink.entrySet()) {
                Id<Link> linkId = entry.getKey();
                Integer link_DTV = entry.getValue();
                Link link = network.getLinks().get(linkId);
                double linkLength = link.getLength();
                double link_mileage = link_DTV * linkLength;

                writer.writeNext(new String[]{String.valueOf(linkId), String.valueOf(link_mileage)});

            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
