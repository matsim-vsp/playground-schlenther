package org.matsim.run.replaceCarByDRT;

import com.opencsv.CSVWriter;
import org.locationtech.jts.geom.prep.PreparedGeometry;
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

    public static void main(String[] args) {
        String inputFile = "scenarios/output/runs-2023-05-26/extraPtPlan-false/drtStopBased-false/massConservation-true/massConservation-1506vehicles-8seats.output_events.xml.gz";
        String inputNetwork = "scenarios/output/runs-2023-05-26/extraPtPlan-false/drtStopBased-false/massConservation-true/massConservation-1506vehicles-8seats.output_network.xml.gz";

        Network network = NetworkUtils.readNetwork(inputNetwork);

        String inner_city_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
        String berlin_shp = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
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
        DTVPerLink2CSV(handler1.getDTVPerLink_Total(),network, innerCity, berlin, outputFileName1);
        //TODO: do the same for mileage, already in eventHandler
    }

    private static void DTVPerLink2CSV(Map<Id<Link>, Integer> DTVPerLink, Network network, List<PreparedGeometry> innerCity_shp, List<PreparedGeometry> berlin_shp, String outputFileName){
        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
            writer.writeNext(new String[]{"link","agents"});

            for (Map.Entry<Id<Link>, Integer> entry : DTVPerLink.entrySet()) {
                Id<Link> linkId = entry.getKey();
                Integer link_DTV = entry.getValue();
                Link link = network.getLinks().get(linkId);
                double linkLength = link.getLength(); //TODO: Mileage

                writer.writeNext(new String[]{String.valueOf(linkId), String.valueOf(link_DTV)});

            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getLocationType(Link link,  List<PreparedGeometry> innerCity_shp, List<PreparedGeometry> berlin_shp) {

        //TODO: is it enough to get the coordinate from the FromNode?
        if(ShpGeometryUtils.isCoordInPreparedGeometries(link.getFromNode().getCoord(), innerCity_shp)){
            return "innerCity";
        } else if (ShpGeometryUtils.isCoordInPreparedGeometries(link.getFromNode().getCoord(), berlin_shp)){
            if(!ShpGeometryUtils.isCoordInPreparedGeometries(link.getFromNode().getCoord(), innerCity_shp)){
                return "BerlinButNotInnerCity";
            }
            else {
                throw new IllegalStateException("inner city should be catched above");
            }
        } else {
            return "Brandenburg";
        }
    }


}
