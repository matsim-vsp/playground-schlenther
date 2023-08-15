/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.run.replaceCarByDRT;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Write DRT stops xml.
 */
public final class DrtStopsWriter extends MatsimXmlWriter {

	private static final Logger log = LogManager.getLogger(DrtStopsWriter.class);

	private static Network network;
	private final URL url2PRStations;
	private final URL url2Shp;

	public static void main(String[] args) throws IOException {
		network = NetworkUtils.readNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz");
		new DrtStopsWriter(network,
				IOUtils.resolveFileOrResource("scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp"),
				IOUtils.resolveFileOrResource("scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-07-27-pr-stations.tsv"))
			.write("scenarios/berlin/replaceCarByDRT/noModeChoice/drtStops/drtStops-hundekopf-carBanArea-2023-07-27-prStations.xml");
	}

	DrtStopsWriter(Network network, URL url2Shp, URL url2PRStations) {
		this.network = network;
		this.url2Shp = url2Shp;
		this.url2PRStations = url2PRStations;
	}

	/**
	 * Write content to specified folder.
	 */
	public void write(String outputFile) throws UncheckedIOException, IOException {
		this.openFile(outputFile);
		this.writeXmlHead();
		this.writeDoctype("transitSchedule", "http://www.matsim.org/files/dtd/transitSchedule_v1.dtd");
		this.writeStartTag("transitSchedule", null);
		this.writeStartTag("transitStops", null);
		this.writeTransitStops(outputFile);
		this.writeEndTag("transitStops");
		this.writeEndTag("transitSchedule");
		this.close();
	}

	private void writeTransitStops(String outputFile) throws IOException {
		String file = outputFile.substring(0, outputFile.lastIndexOf(".xml")) + ".csv";
		// Write csv file for adjusted stop location
		log.info("writing to " + file);
		FileWriter csvWriter = new FileWriter(file);
		csvWriter.append("Stop ID");
		csvWriter.append(",");
		csvWriter.append("Link ID");
		csvWriter.append(",");
		csvWriter.append("X");
		csvWriter.append(",");
		csvWriter.append("Y");
		csvWriter.append("\n");

		// Read original data csv
		log.info("Start processing the network. This may take some time...");

		//get all links with to Node in Area
		final List<PreparedGeometry> preparedGeometries = ShpGeometryUtils.loadPreparedGeometries(url2Shp);
		Set<? extends Link> linksInArea = network.getLinks()
				.values()
				.stream()
				.filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getToNode().getCoord(),
						preparedGeometries))
				.collect(Collectors.toSet());
		try {
			//write a stop for each filtered link (with to node in area) - TODO: check if linkId contains "pt"
			for (Link link : linksInArea) {
				if(!link.getId().toString().contains("pt")){
					writeStop(csvWriter, link);
				}
			}
			//write a stop for each P+R link - TODO: check if linkId already exists
			Set<PRStation> prStations = ReplaceCarByDRT.readPRStationFile(url2PRStations);

			for (PRStation prStation : prStations) {

				boolean linkIdExists = linksInArea.stream()
						.map(Link::getId)
						.anyMatch(id -> id.equals(prStation.getLinkId()));

				if (!linkIdExists) {
					writeStop(csvWriter, network.getLinks().get(prStation.getLinkId()));
				}
			}

		} catch (IOException e){
			e.printStackTrace();
		}

		csvWriter.close();
	}

	private void writeStop(FileWriter csvWriter, Link link) throws IOException {

		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>(5);
		attributes.add(createTuple("id", link.getId().toString()));
		attributes.add(createTuple("x", link.getToNode().getCoord().getX()));
		attributes.add(createTuple("y", link.getToNode().getCoord().getY()));
		attributes.add(createTuple("linkRefId", link.getId().toString()));
		this.writeStartTag("stopFacility", attributes, true);

		csvWriter.append(link.getId().toString());
		csvWriter.append(",");
		csvWriter.append(link.getId().toString());
		csvWriter.append(",");
		csvWriter.append(Double.toString(link.getToNode().getCoord().getX()));
		csvWriter.append(",");
		csvWriter.append(Double.toString(link.getToNode().getCoord().getY()));
		csvWriter.append("\n");
	}

}
