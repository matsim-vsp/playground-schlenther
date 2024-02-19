package org.matsim.analysis;

import com.opencsv.CSVWriter;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class RunTrafficVolumeEventHandler {

    private final String runDirectory;
    private final String runId;
    private final String inner_city_shp;
    private final String berlin_shp;

    private static final Logger log = Logger.getLogger(RunTripsPreparation.class);

    public RunTrafficVolumeEventHandler(String runDirectory, String runId, String inner_city_shp, String berlin_shp, String boundary_shp){
        this.runDirectory = runDirectory;
        this.runId = runId;
        this.inner_city_shp = inner_city_shp;
        this.berlin_shp = berlin_shp;
    }

    public static void main(String[] args) {
        if(args.length == 0) {
            String runDirectory = "scenarios/output/runs-2023-09-01/10pct/noDRT/";
            String runId = "noDRT";
            String inner_city_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            String berlin_shp = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
            String boundary_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-boundaries-500m.shp";

            RunTrafficVolumeEventHandler trafficVolumes = new RunTrafficVolumeEventHandler(runDirectory, runId, inner_city_shp, berlin_shp, boundary_shp);
            trafficVolumes.run();
        } else {
            String runDirectory = args[0];
            String runId = args[1];
            String inner_city_shp = args[2];
            String berlin_shp = args[3];
            String boundary_shp = args[4];

            RunTrafficVolumeEventHandler trafficVolumes = new RunTrafficVolumeEventHandler(runDirectory, runId, inner_city_shp, berlin_shp, boundary_shp);
            trafficVolumes.run();

        }
    }

    public void run() {
        String inputFile = runDirectory + runId + ".output_events.xml.gz";
        String inputNetwork = runDirectory + runId + ".output_network.xml.gz";

        Network network = NetworkUtils.readNetwork(inputNetwork);

        List<PreparedGeometry> innerCity = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(inner_city_shp));
        List<PreparedGeometry> berlin = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(berlin_shp));
        List<PreparedGeometry> boundary = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(berlin_shp));


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
        String HTVOutputFileName = inputFile.substring(0, inputFile.lastIndexOf(".xml")) + "_hourlyTrafficVolume_vehicles.tsv";
        DTVPerLink2CSV(handler1.getDTVPerLink_Total(), handler1.getDTVPerLink_DRT(), handler1.getDTVPerLink_nonDRT(), network, innerCity, berlin, boundary, outputFileName1);
        mileagePerLink2CSV(handler1.getDTVPerLink_Total(), handler1.getDTVPerLink_DRT(), handler1.getDTVPerLink_nonDRT(), network, innerCity, berlin, boundary, mileageOutputFileName);
        HTVPerLink2CSV(handler1.getHTVPerLink_Total(), network, innerCity, berlin, HTVOutputFileName);
    }

    private static void DTVPerLink2CSV(Map<Id<Link>, Integer> DTVPerLink, Map<Id<Link>, Integer> DTVPerLink_DRT, Map<Id<Link>, Integer> DTVPerLink_nonDRT,
                                       Network network, List<PreparedGeometry> innerCity_shp, List<PreparedGeometry> berlin_shp, List<PreparedGeometry> boundary_shp, String outputFileName){
        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
            writer.writeNext(new String[]{"link","agents","DRT","nonDRT","zone","isInBoundaryZone","roadtype"});

            for (Map.Entry<Id<Link>, Integer> entry : DTVPerLink.entrySet()) {
                Id<Link> linkId = entry.getKey();
                Link link = network.getLinks().get(linkId);
                String region = getRegion(link,innerCity_shp,berlin_shp);
                String roadtype = (String) link.getAttributes().getAttribute("type");
                boolean isInBoundaryZone = ShpGeometryUtils.isCoordInPreparedGeometries(link.getFromNode().getCoord(), boundary_shp);

                Integer link_DTV = entry.getValue();
                Integer link_DTV_DRT = DTVPerLink_DRT.get(linkId);
                Integer link_DTV_nonDRT = DTVPerLink_nonDRT.get(linkId);
                writer.writeNext(new String[]{String.valueOf(linkId), String.valueOf(link_DTV), String.valueOf(link_DTV_DRT), String.valueOf(link_DTV_nonDRT), region, String.valueOf(isInBoundaryZone), roadtype});
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void HTVPerLink2CSV(Map<Id<Link>, Map<Double, Integer>> HTVPerLink_Total,
                                       Network network, List<PreparedGeometry> innerCity_shp, List<PreparedGeometry> berlin_shp, String outputFileName){
        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', '\0', '\0', "\n");

            StringBuilder lineBuilder = new StringBuilder();
            lineBuilder.append("link\t");
            for (double i = 1.0; i <= 36.0; i += 1.0) {
                String formattedTime = String.format("%02d:%02d", (int)i, (int)((i % 1) * 60));
                lineBuilder.append(formattedTime);
                lineBuilder.append('\t');
            }
            writer.writeNext(new String[]{lineBuilder.toString()});

            for (Map.Entry<Id<Link>, Map<Double, Integer>> entry : HTVPerLink_Total.entrySet()) {
                Id<Link> linkId = entry.getKey();
                Map<Double, Integer> innerMap = entry.getValue();
                StringBuilder lineBuilder2 = new StringBuilder();
                lineBuilder2.append(linkId.toString());
                lineBuilder2.append("\t");

                for (double i = 1.0; i <= 36.0; i += 1.0) {
                    lineBuilder2.append(innerMap.get(i));
                    lineBuilder2.append('\t');
                }
                writer.writeNext(new String[]{lineBuilder2.toString()});
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void mileagePerLink2CSV(Map<Id<Link>, Integer> DTVPerLink, Map<Id<Link>, Integer> DTVPerLink_DRT, Map<Id<Link>, Integer> DTVPerLink_nonDRT,
                                           Network network, List<PreparedGeometry> innerCity_shp, List<PreparedGeometry> berlin_shp, List<PreparedGeometry> boundary_shp, String outputFileName){
        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
            writer.writeNext(new String[]{"link","mileage","DRT","nonDRT","zone","roadtype"});

            for (Map.Entry<Id<Link>, Integer> entry : DTVPerLink.entrySet()) {
                Id<Link> linkId = entry.getKey();
                Integer link_DTV = entry.getValue();
                Link link = network.getLinks().get(linkId);
                String region = getRegion(link,innerCity_shp,berlin_shp);
                String roadtype = (String) link.getAttributes().getAttribute("type");

                double link_mileage = link_DTV * link.getLength();
                double link_mileage_DRT = DTVPerLink_DRT.get(linkId) * link.getLength();
                double link_mileage_nonDRT = DTVPerLink_nonDRT.get(linkId) * link.getLength();

                writer.writeNext(new String[]{String.valueOf(linkId), String.valueOf(link_mileage), String.valueOf(link_mileage_DRT), String.valueOf(link_mileage_nonDRT), region, roadtype});

            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static String getRegion(Link link, List<PreparedGeometry> innerCity, List<PreparedGeometry> berlin) {

        if(ShpGeometryUtils.isCoordInPreparedGeometries(link.getFromNode().getCoord(), innerCity)){
            return "innerCity";
        }

        if(ShpGeometryUtils.isCoordInPreparedGeometries(link.getFromNode().getCoord(), berlin)){
            if(!ShpGeometryUtils.isCoordInPreparedGeometries(link.getFromNode().getCoord(), innerCity)){
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
