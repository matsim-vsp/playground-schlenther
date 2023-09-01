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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class PRStation {

	private final String name;
	private final Id<Link> linkId;
	private final Coord coord;
	private static final Logger LOG = Logger.getLogger(PRStation.class);

	PRStation(String name, Id<Link> linkId, Coord coord) {
		this.name = name;
		this.linkId = linkId;
		this.coord = coord;
	}

	/**
	 *
	 * @param url2PRStations a .tsv input file with the following columns (and a header row): 'name', 'x', 'y' and 'linkId'. The order should not matter.
	 * @return
	 */
	static Set<PRStation> readPRStationFile(URL url2PRStations) {
		LOG.info("read input file for P+R stations");
		Set<PRStation> prStations = new HashSet<>();
		//assume tsv with a header and linkId in the last column
		try {
			CSVParser parser = CSVParser.parse(IOUtils.getBufferedReader(url2PRStations), CSVFormat.DEFAULT.withDelimiter('\t').withFirstRecordAsHeader());
			Map<String, Integer> headerMap = parser.getHeaderMap();
			parser.getRecords().forEach(record -> {
				String name = record.get(headerMap.get("name"));
				Id<Link> linkId = Id.createLinkId(record.get(headerMap.get("linkId")));
				Coord coord = new Coord(Double.parseDouble(record.get(headerMap.get("x"))), Double.parseDouble(record.get(headerMap.get("y"))));
				prStations.add(new PRStation(name, linkId, coord));
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return prStations;
	}

	protected Coord getCoord() {
		return coord;
	}

	protected String getName() {
		return name;
	}

	protected Id<Link> getLinkId() {
		return linkId;
	}
}
