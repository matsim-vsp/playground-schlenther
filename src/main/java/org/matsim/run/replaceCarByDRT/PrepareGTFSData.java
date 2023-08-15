package org.matsim.run.replaceCarByDRT;

import com.opencsv.CSVWriter;
import org.apache.log4j.Logger;
import org.matsim.analysis.RunTripsPreparation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.common.util.StraightLineKnnFinder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.replaceCarByDRT.PRStation;
import org.matsim.run.replaceCarByDRT.ReplaceCarByDRT;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.valueOf;

public class PrepareGTFSData {

    public static void main(String[] args) {
        String stops_file = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/stops.tsv";
        String input_network = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";

        String outputFileName = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-08-11-pr-stations-outside.tsv";

        Set<PRStation> stops = ReplaceCarByDRT.readPRStationFile(IOUtils.resolveFileOrResource(stops_file));
        Network network = NetworkUtils.readNetwork(input_network);

        // Remove all pt links
        for(Map.Entry<Id<Link>, ? extends Link> entry : network.getLinks().entrySet()) {

            Id<Link> linkId = entry.getKey();
            Link link = entry.getValue();

            if(link.getAllowedModes().contains("pt")) {
                network.removeLink(linkId);
            }
        }

        // Remove all pt nodes
        for(Map.Entry<Id<Node>, ? extends Node> entry : network.getNodes().entrySet()) {

            Id<Node> nodeId = entry.getKey();
            Node node = entry.getValue();

            if(node.toString().contains("pt")) {
                network.removeNode(nodeId);
            }
        }

        // Set linkIds to nearest Link
        for(PRStation prStation : stops) {

            Coord coord = prStation.getCoord();
            Link link = NetworkUtils.getNearestLink(network, coord);

            Id<Link> linkId = link.getId();

            if (linkId != null) {
                prStation.setLinkId(linkId);
            }
        }

        // Write to tsv-file
        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
            writer.writeNext(new String[]{"name",
                    "x",
                    "y",
                    "linkId"});

            for(PRStation prStation : stops) {
                writer.writeNext(new String[]{
                    prStation.getName(),
                    valueOf(prStation.getCoord().getX()),
                    valueOf(prStation.getCoord().getY()),
                    valueOf(prStation.getLinkId())
                });
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
