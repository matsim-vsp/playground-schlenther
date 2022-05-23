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

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.Set;

class ReplaceCarByDRT {

	private static Logger log = Logger.getLogger(ReplaceCarByDRT.class);


	//TODO think about using plan types
	static void replaceInnerTripsOfModesInAreaByMode(Scenario scenario,
													 Set<String> modesToBeReplaced,
													 String replacingMode,
													 URL url2CarFreeSingleGeomShapeFile,
													 MainModeIdentifier mainModeIdentifier){

		// First check whether we can properly interpret the shape file.
		// If it contained more than one geom, we would have to make other queries on order to alter only inner trips (i.e. not use ShpGeometryUtils)
		List<PreparedGeometry> carFreeGeoms = ShpGeometryUtils.loadPreparedGeometries(url2CarFreeSingleGeomShapeFile);
		Preconditions.checkArgument(carFreeGeoms.size() == 1, "you have to provide a shape file that features exactly one geometry.");

		log.info("start modifying input plans....");
		PopulationFactory fac = scenario.getPopulation().getFactory();
		MutableInt replacedTrips = new MutableInt();

		scenario.getPopulation().getPersons().values().stream()
				.flatMap(person -> person.getPlans().stream())
				.forEach(plan -> {
					TripStructureUtils.getTrips(plan).stream()
							.forEach(trip -> {
								String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
								if(modesToBeReplaced.contains(mainMode) && isInnerTrip(scenario, trip, carFreeGeoms)){
									List<PlanElement> newTrip = List.of(fac.createLeg(replacingMode));
									TripRouter.insertTrip(plan.getPlanElements(), trip.getOriginActivity(), newTrip, trip.getDestinationActivity());
									replacedTrips.increment();
								}
							});
				});
		log.info("nr of trips replaced = " + replacedTrips);
		log.info("finished modifying input plans....");
	}

	private static boolean isInnerTrip(Scenario scenario, TripStructureUtils.Trip trip, List<PreparedGeometry> geoms){
		return isActivityInGeoms(scenario, trip.getOriginActivity(), geoms) && isActivityInGeoms(scenario, trip.getDestinationActivity(), geoms);
	}

	private static boolean isActivityInGeoms(Scenario scenario, Activity activity, List<PreparedGeometry> geoms){
		Coord coord = activity.getCoord();
		if(coord == null){
			coord = FacilitiesUtils.decideOnCoord(scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId()),
					scenario.getNetwork(), scenario.getConfig());
		}
		return ShpGeometryUtils.isCoordInPreparedGeometries(coord,geoms);
	}

}
