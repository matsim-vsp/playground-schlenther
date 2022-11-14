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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.common.util.StraightLineKnnFinder;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

class ReplaceCarByDRT {

	private static Logger log = Logger.getLogger(ReplaceCarByDRT.class);

	static Id<Link> PR_SUEDKREUZ = Id.createLinkId(123744);
	static Id<Link> PR_GESUNDBRUNNEN = Id.createLinkId(18796);
	static Id<Link> PR_OSTKREUZ = Id.createLinkId(125468);
	static Id<Link> PR_ZOB = Id.createLinkId(59825); //aka Westkreuz

	static final String TRIP_TYPE_ATTR_KEY = "tripType";
	static final String PR_ACTIVITY_TYPE = "P+R";


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
								if(modesToBeReplaced.contains(mainMode) && getTripType(scenario, trip, carFreeGeoms).equals(TripType.innerTrip)){
									List<PlanElement> newTrip = List.of(fac.createLeg(replacingMode));
									TripRouter.insertTrip(plan.getPlanElements(), trip.getOriginActivity(), newTrip, trip.getDestinationActivity());
									replacedTrips.increment();
								}
							});
				});
		log.info("nr of trips replaced = " + replacedTrips);
		log.info("finished modifying input plans....");
	}

	static void replaceModeTripsInsideAreaAndSplitBorderCrossingTripsAtBorderLinks(Scenario scenario,
																				   Set<String> modesToBeReplaced,
																				   String replacingMode,
																				   URL url2CarFreeSingleGeomShapeFile,
																				   Set<Id<Link>> borderLinkIds,
																				   MainModeIdentifier mainModeIdentifier,
																				   PRStationChoice prStationChoice){

		// First check whether we can properly interpret the shape file.
		// If it contained more than one geom, we would have to make other queries on order to alter only inner trips (i.e. not use ShpGeometryUtils)
		List<PreparedGeometry> carFreeGeoms = ShpGeometryUtils.loadPreparedGeometries(url2CarFreeSingleGeomShapeFile);
		Preconditions.checkArgument(carFreeGeoms.size() == 1, "you have to provide a shape file that features exactly one geometry.");
		Preconditions.checkArgument(prStationChoice.equals(PRStationChoice.closestToOutSideActivity) || prStationChoice.equals(PRStationChoice.closestToInsideActivity), "do not know what to do with " + prStationChoice);

		log.info("start modifying input plans....");
		PopulationFactory fac = scenario.getPopulation().getFactory();
		MutableInt replacedTrips = new MutableInt();

		Set<Link> borderLinks = scenario.getNetwork().getLinks().values().stream()
				.filter(link -> borderLinkIds.contains(link.getId()))
				.collect(Collectors.toSet());

		StraightLineKnnFinder<Activity,Link> straightLineKnnFinder = new StraightLineKnnFinder<>(1, Activity::getCoord, l -> l.getToNode().getCoord());

		log.warn("will assume that the first activity of each person is the home activity. This holds true for the open Berlin scenario. For other scenarios, please check !!");

		for (Person person : scenario.getPopulation().getPersons().values()) {

			//person attribute
			Activity homeAct = (Activity) person.getSelectedPlan().getPlanElements().get(0); //in Berlin, the first activity of each person is the home activity. Careful: this might not be the case in other scenarios!!
			if (PopulationUtils.getSubpopulation(person).equals("person") && !homeAct.getType().startsWith("home")){
				throw new IllegalArgumentException("first act of agent " + person.getId() + " is not home");
			}
			Boolean livesInProhibitionZone = ShpGeometryUtils.isCoordInPreparedGeometries(homeAct.getCoord(), carFreeGeoms) ? true : false;
			PopulationUtils.putPersonAttribute(person, "livesInProhibitionZone", livesInProhibitionZone);

			for (Plan plan : person.getPlans()) {

				//will in fact put an attribute into the origin activity
				List<TripStructureUtils.Trip> tripsToReplace = collectAndAttributeTripsToReplace(scenario, plan, mainModeIdentifier, modesToBeReplaced, carFreeGeoms);
				if (tripsToReplace.isEmpty()) continue; //nothing to do; skip the plan

				//for consistency checking
				long nrOfBorderCrossingCarTrips = tripsToReplace.stream()
						.filter(trip -> mainModeIdentifier.identifyMainMode(trip.getTripElements()).equals(TransportMode.car))
						.filter(trip -> trip.getTripAttributes().getAttribute(TRIP_TYPE_ATTR_KEY).equals(TripType.endingTrip) || trip.getTripAttributes().getAttribute(TRIP_TYPE_ATTR_KEY).equals(TripType.originatingTrip))
						.count();
				long nrOfOutsideTrips = tripsToReplace.stream()
						.filter(trip -> trip.getTripAttributes().getAttribute(TRIP_TYPE_ATTR_KEY).equals(TripType.outsideTrip))
						.count();
				if(nrOfOutsideTrips == tripsToReplace.size()){
					continue; //this agent is not affected by the prohibition zone.
				}

				Id<Link> firstPRStation = null;
				//we use this as 'iteration variable'
				Id<Link> lastCarPRStation = null;

				for (TripStructureUtils.Trip trip : tripsToReplace) {
					TripType tripType = (TripType) trip.getTripAttributes().getAttribute(TRIP_TYPE_ATTR_KEY);
					String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
					if (tripType.equals(TripType.outsideTrip)){
						if(mainMode.equals(TransportMode.car) && lastCarPRStation != null) throw new IllegalStateException("agent " + person + "performs an outside trip from " + trip.getOriginActivity() + "to " + trip.getDestinationActivity() +
								"\n but vehicle is still parked at link=" + lastCarPRStation);
						continue;
					}

					List<PlanElement> newTrip;
					Id<Link> prStation = null;

					if(tripType.equals(TripType.innerTrip)) {
						newTrip = List.of(fac.createLeg(replacingMode));
					} else if(tripType.equals(TripType.originatingTrip)) {
						if(mainMode.equals(TransportMode.car)){
							nrOfBorderCrossingCarTrips --;
							//some consistency (mass conservation) checks
							if(nrOfBorderCrossingCarTrips == 0 && livesInProhibitionZone){
								throw new IllegalStateException("agent " + person.getId() + "lives inside but travels outside the border with car without returning with a prohibited mode.\n" +
										"trip = " + trip);
							}
							//car has to be picked up where it was left the last time
							if (lastCarPRStation != null){
								prStation = lastCarPRStation;
							}
						}
					 	if(prStation == null){ //if no car trip into zone was observed before or if the mode is ride, we enter here
							Activity act = prStationChoice.equals(PRStationChoice.closestToInsideActivity) ? trip.getOriginActivity() : trip.getDestinationActivity();
							prStation =  straightLineKnnFinder.findNearest(act, borderLinks.stream())
									.stream()
									.findFirst()
									.orElseThrow()
									.getId();
						}

						lastCarPRStation = null;

						Activity parkAndRideAct = fac.createActivityFromLinkId(PR_ACTIVITY_TYPE, prStation);
						parkAndRideAct.setMaximumDuration(5 * 60);
						newTrip = List.of(fac.createLeg(replacingMode),
								parkAndRideAct,
								fac.createLeg(mainMode));

					} else if (tripType.equals(TripType.endingTrip)) {
						if(mainMode.equals(TransportMode.car)){
							nrOfBorderCrossingCarTrips --;
							if(nrOfBorderCrossingCarTrips == 0){
								//some consistency (mass conservation) checks
								if(!livesInProhibitionZone){
									throw new IllegalStateException("agent " + person.getId() + " lives outside but travels into the prohibition zone with car without returning with a prohibited mode.\n" +
											"trip = " + trip);
								}
								//agents needs to park the car where it will be picked up at the start of the next iteration, i.e. next day.
								prStation = firstPRStation;
							}
						}
					 	if(prStation == null) { //if not the last border-crossing car or a ride trip
							Activity act = prStationChoice.equals(PRStationChoice.closestToInsideActivity) ? trip.getDestinationActivity() : trip.getOriginActivity();
							prStation = straightLineKnnFinder.findNearest(act, borderLinks.stream())
									.stream()
									.findFirst()
									.orElseThrow()
									.getId();
							if(mainMode.equals(TransportMode.car)) lastCarPRStation = prStation;
						}

						Activity parkAndRideAct = fac.createActivityFromLinkId(PR_ACTIVITY_TYPE, prStation);
						parkAndRideAct.setMaximumDuration(5 * 60);
						newTrip = List.of(fac.createLeg(mainMode),
									parkAndRideAct,
									fac.createLeg(replacingMode));
					} else {
						throw new IllegalArgumentException("unknown trip type: " + tripType);
					}
					//change value of firstPRStation only one time
					firstPRStation = firstPRStation == null ? lastCarPRStation : firstPRStation;
					//insert new trip into plan
					TripRouter.insertTrip(plan.getPlanElements(), trip.getOriginActivity(), newTrip, trip.getDestinationActivity());
					replacedTrips.increment();
				}
			}
		}
		log.info("nr of trips replaced = " + replacedTrips);
		log.info("finished modifying input plans....");
	}

	/**
	 * retrieves the trips in the plan of modesToBeReplaced and attributes them with the corresponding TripType
	 * @param plan
	 * @param mainModeIdentifier
	 * @param modesToBeReplaced
	 * @param prohibitionZoneGeoms
	 * @return
	 */
	private static List<TripStructureUtils.Trip> collectAndAttributeTripsToReplace(Scenario scenario, Plan plan, MainModeIdentifier mainModeIdentifier, Set<String> modesToBeReplaced, List<PreparedGeometry> prohibitionZoneGeoms) {
		//have to use list and not a set because otherwise the order might get messed up
		List<TripStructureUtils.Trip> trips = new ArrayList<>();
		List<TripStructureUtils.Trip> allTrips = TripStructureUtils.getTrips(plan);
		for (TripStructureUtils.Trip trip : allTrips) {
			String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
			if (modesToBeReplaced.contains(mainMode)) {
				//will in fact put an attribute into the origin activity
				trip.getTripAttributes().putAttribute(TRIP_TYPE_ATTR_KEY, getTripType(scenario, trip, prohibitionZoneGeoms));
				trips.add(trip);
			}
		}

//		Collections.sort(trips);
//
//		//for a reason that is unclear to me (TS), trips are not automatically sorted chronologically...
//		Collections.sort(trips, new Comparator<TripStructureUtils.Trip>() {
//			@Override
//			public int compare(TripStructureUtils.Trip o1, TripStructureUtils.Trip o2) {
//				return o1.getLegsOnly().get(0).getDepartureTime() ;
//			}
//		});
		return trips;
	}


	private static boolean isActivityInGeoms(Scenario scenario, Activity activity, List<PreparedGeometry> geoms){
		Coord coord = activity.getCoord();
		if(coord == null){
			coord = FacilitiesUtils.decideOnCoord(scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId()),
					scenario.getNetwork(), scenario.getConfig());
		}
		return ShpGeometryUtils.isCoordInPreparedGeometries(coord,geoms);
	}

	private static TripType getTripType(Scenario scenario, TripStructureUtils.Trip trip, List<PreparedGeometry> geoms){
		boolean originatingInside = isActivityInGeoms(scenario, trip.getOriginActivity(), geoms);
		boolean endingInside = isActivityInGeoms(scenario, trip.getDestinationActivity(), geoms);
		if(originatingInside && endingInside) return TripType.innerTrip;
		if(originatingInside && !endingInside) return TripType.originatingTrip;
		if(!originatingInside && endingInside) return TripType.endingTrip;
		if(!originatingInside && !endingInside) return TripType.outsideTrip;
		else {
			throw new RuntimeException("should not happen");
		}
	}

	/**
	 *  1) modifies the allowedModes of all links within {@code carFreeGeoms} except for motorways such that they do not contain car neither ride.
	 *  2) cleans the network
	 *  3) deletes all car routes in the population, that touch the forbidden links
	 *
	 *
	 * @param scenario
	 * @param url2CarFreeSingleGeomShapeFile
	 */
	static final void banCarAndRideFromNetworkArea(Scenario scenario, URL url2CarFreeSingleGeomShapeFile, Set<String> excludedRoadTypes){
		List<PreparedGeometry> carFreeGeoms = ShpGeometryUtils.loadPreparedGeometries(url2CarFreeSingleGeomShapeFile);

		Set<Id<Link>> forbiddenLinks = scenario.getNetwork().getLinks().values().parallelStream()
				.filter(l -> l.getAllowedModes().contains(TransportMode.car))
				.filter(l -> {
					String type = ((String) (l.getAttributes().getAttribute("type")));
					return !(excludedRoadTypes.stream()
							.filter(excludedType -> type.contains(excludedType))
							.findAny()
							.isPresent());}) // cars remain allowed on excludedRoadTypes
				.filter(l -> ShpGeometryUtils.isCoordInPreparedGeometries(l.getToNode().getCoord(), carFreeGeoms))
				.map(l -> l.getId())
				.collect(Collectors.toSet());

		forbiddenLinks.forEach(id -> {
			Link l = scenario.getNetwork().getLinks().get(id);
			Set<String> allowedModes = new HashSet<>(l.getAllowedModes());
			allowedModes.remove(TransportMode.car);
			allowedModes.remove(TransportMode.ride);
			l.setAllowedModes(allowedModes);
		});
		log.info("clean car network");
		cleanModalNetwork(scenario.getNetwork(),TransportMode.car);
		log.info("clean ride network");
		cleanModalNetwork(scenario.getNetwork(),TransportMode.ride);

		deleteCarRoutesThatHaveForbiddenLinks(scenario.getPopulation(), forbiddenLinks);
	}

	private static void cleanModalNetwork(Network network, String mode) {
		Set<String> modes = new HashSet<>();
		modes.add(mode);
		new MultimodalNetworkCleaner(network).run(modes);
		log.info("finished");
	}

	private static void deleteCarRoutesThatHaveForbiddenLinks(Population population, Set<Id<Link>> forbiddenLinks) {
		log.info("start deleting every car route that travels one or more links within car-free-zone");

		population.getPersons().values().stream()
				.forEach(person -> person.getPlans().stream().flatMap(plan ->
						TripStructureUtils.getLegs(plan).stream())
						.forEach(leg -> {
							if(leg.getMode().equals(TransportMode.car)){
								Route route = leg.getRoute();
								boolean routeTouchesZone = (route instanceof NetworkRoute && ((NetworkRoute) route).getLinkIds().stream().filter(l -> forbiddenLinks.contains(l)).findAny().isPresent() );
								if(routeTouchesZone || forbiddenLinks.contains(route.getStartLinkId()) || forbiddenLinks.contains(route.getEndLinkId()) ){
									leg.setRoute(null);
								}
							}
						}));

		log.info(".... finished deleting every car route that travels one or more links within car-free-zone");
	}


	private enum TripType{
		innerTrip, originatingTrip, endingTrip, outsideTrip
	}

	enum PRStationChoice{
		closestToOutSideActivity, closestToInsideActivity
	}

}
