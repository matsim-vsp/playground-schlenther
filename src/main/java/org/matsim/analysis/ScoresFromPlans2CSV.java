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

package org.matsim.analysis;

import com.opencsv.CSVWriter;
import com.opencsv.bean.util.OpencsvUtils;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ScoresFromPlans2CSV {

	private static final String INPUT_POPULATION = "D:/svn/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-1pct/output-berlin-v5.5-1pct/berlin-v5.5.3-1pct.output_plans.xml.gz";
	private static final String INPUT_INNER_CITY_SHP = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
	private static final String INPUT_BERLIN_SHP = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";

	public static void main(String[] args) {

		Population population = PopulationUtils.readPopulation(INPUT_POPULATION);
		String outputFileName = INPUT_POPULATION.substring(0, INPUT_POPULATION.lastIndexOf(".xml")) + "_selectedPlanScores.tsv";

		List<PreparedGeometry> innerCity = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(INPUT_INNER_CITY_SHP));
		List<PreparedGeometry> berlin = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(INPUT_BERLIN_SHP));

		try {
			CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
			writer.writeNext(new String[]{"agentId",
					"selectedPlanScore",
					"livesInBerlin",
					"livesInInnerCity"});
			for (Person person : population.getPersons().values()) {

				Activity home = getHomeActivity(person.getSelectedPlan());

				boolean livesInInnerCity = false;
				boolean livesInBerlin = false;

				if(ShpGeometryUtils.isCoordInPreparedGeometries(home.getCoord(), innerCity)){
					livesInInnerCity = true;
				}
				if(ShpGeometryUtils.isCoordInPreparedGeometries(home.getCoord(), berlin)){
					livesInBerlin = true;
				}

				writer.writeNext(new String[]{person.getId().toString(),
						String.valueOf(person.getSelectedPlan().getScore()),
								String.valueOf(livesInBerlin),
								String.valueOf(livesInInnerCity)}
				);
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Activity getHomeActivity(Plan selectedPlan) {
		List<Activity> acts = TripStructureUtils.getActivities(selectedPlan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

		return acts.stream()
				.filter(act -> act.getType().contains("home"))
				.findFirst().orElse(acts.get(0));
	}

}




