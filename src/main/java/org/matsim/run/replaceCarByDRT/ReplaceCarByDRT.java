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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

class ReplaceCarByDRT {

	private static Logger log = Logger.getLogger(ReplaceCarByDRT.class);

	static final String TRIP_TYPE_ATTR_KEY = "tripType";
	static final String PR_ACTIVITY_TYPE = "P+R";

	/**
	 *
	 * @param scenario
	 * @param modesToBeReplaced
	 * @param replacingMode
	 * @param url2CarFreeSingleGeomShapeFile
	 * @param mainModeIdentifier
	 */
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

	/**
	 * Multiple side effects!! <br>
	 * Sets a plan type for all plans. <br>
	 * Plans that use car and touch the prohibitoin zone provided by {@code url2CarFreeSingleGeomShapeFile} are mutated and copied. <br>
	 * Those plans are mutated such that agents use a P+R logic instead, meaning they drive to/from P+R stations provided in {@code url2PRStations} with car and choose one of {@code replacingModes} inside the prohibition area. <br>
	 * Makes a copy of each P+R plan per replacingMode and assigns a corresponding plan type, such that agents can choose between the replacing modes (pseudo mode choice). <br>
	 * So if replacingModes is {drt,pt} and agent had 1 original plan, it will then have 2 plans, one of type 'drt' and one of type 'pt'. That means, the original plan is thrown away.
	 *
	 * Additionally, if {@code extraPTPlan} is true, another plan is added where the agent does not use P+R logic but just uses pt for all trips in all subtours that touched the prohibition zone with car, <br>
	 * or for all trips that used ride inside the prohibition zone (possible in combination with other modes in the same subtour).
	 * @param scenario
	 * @param replacingModes
	 * @param url2CarFreeSingleGeomShapeFile
	 * @param url2PRStations
	 * @param mainModeIdentifier
	 * @param prStationChoice
	 * @param extraPTPlan
	 * @param kPrStations
	 */
	static void prepareInputPlansForCarProhibitionWithPRLogic(Scenario scenario,
															  Set<String> replacingModes,
															  URL url2CarFreeSingleGeomShapeFile,
															  URL url2PRStations,
															  MainModeIdentifier mainModeIdentifier,
															  PRStationChoice prStationChoice,
															  boolean enforceMassConservation,
															  boolean extraPTPlan,
															  int kPrStations){

		// First check whether we can properly interpret the shape file.
		// If it contained more than one geom, we would have to make other queries on order to alter only inner trips (i.e. not use ShpGeometryUtils)
		log.info("read input file for car ban area");
		List<PreparedGeometry> carFreeGeoms = ShpGeometryUtils.loadPreparedGeometries(url2CarFreeSingleGeomShapeFile);
		Preconditions.checkArgument(carFreeGeoms.size() == 1, "you have to provide a shape file that features exactly one geometry.");
		Preconditions.checkArgument(prStationChoice.equals(PRStationChoice.closestToOutSideActivity) || prStationChoice.equals(PRStationChoice.closestToInsideActivity), "do not know what to do with " + prStationChoice);
		Preconditions.checkArgument(replacingModes.size() > 0);
		Set<PRStation> prStations = readPRStationFile(url2PRStations);

		log.info("start modifying input plans....");
		PopulationFactory fac = scenario.getPopulation().getFactory();
		MutableInt replacedTrips = new MutableInt();

		Random rnd = MatsimRandom.getRandom();

		StraightLineKnnFinder<Activity,PRStation> straightLineKnnFinder = new StraightLineKnnFinder<>(kPrStations, Activity::getCoord, PRStation::getCoord);
		log.warn("will assume that the first activity of each person is the home activity. This holds true for the open Berlin scenario. For other scenarios, please check !!");

		for (Person person : scenario.getPopulation().getPersons().values()) {

			//person attribute
			Activity homeAct = (Activity) person.getSelectedPlan().getPlanElements().get(0); //in Berlin, the first activity of each person is the home activity. Careful: this might not be the case in other scenarios!!
			if (PopulationUtils.getSubpopulation(person).equals("person") && !homeAct.getType().startsWith("home")){
				throw new IllegalArgumentException("first act of agent " + person.getId() + " is not home");
			}
			Boolean livesInProhibitionZone = ShpGeometryUtils.isCoordInPreparedGeometries(homeAct.getCoord(), carFreeGeoms) ? true : false;
			PopulationUtils.putPersonAttribute(person, "livesInProhibitionZone", livesInProhibitionZone);


			//original plan will be thrown away if agent crosses border with forbidden mode
			Iterator<String> replacingModeIterator = replacingModes.iterator();
			String replacingMode = replacingModeIterator.next();
			Set<Plan> plansToAdd = new HashSet<>();

			for (Plan plan : person.getPlans()) {

				//will in fact put an attribute into the origin activity
				List<TripStructureUtils.Trip> tripsToReplace = collectAndAttributeTripsToReplace(scenario, plan, mainModeIdentifier, Set.of(TransportMode.car, TransportMode.ride), carFreeGeoms);
				if (tripsToReplace.isEmpty()){
					plan.setType("not-affected");
					continue; //nothing to do; skip the plan
				}

				//for consistency checking
				long nrOfBorderCrossingCarTrips = tripsToReplace.stream()
						.filter(trip -> mainModeIdentifier.identifyMainMode(trip.getTripElements()).equals(TransportMode.car))
						.filter(trip -> trip.getTripAttributes().getAttribute(TRIP_TYPE_ATTR_KEY).equals(TripType.endingTrip) || trip.getTripAttributes().getAttribute(TRIP_TYPE_ATTR_KEY).equals(TripType.originatingTrip))
						.count();
				long nrOfOutsideTripsWithModesToReplace = tripsToReplace.stream()
						.filter(trip -> trip.getTripAttributes().getAttribute(TRIP_TYPE_ATTR_KEY).equals(TripType.outsideTrip))
						.count();
				if(nrOfOutsideTripsWithModesToReplace == tripsToReplace.size()){
					plan.setType("not-affected");
					continue; //this agent is not affected by the prohibition zone.
				}

				//create and add a plan, where all the trips to replace are NOT split up with P+R logic but are just replaced by a pt trip
				if(extraPTPlan){
					if(nrOfBorderCrossingCarTrips != 0){
//					we have checked whether the plan contains only external trips, above. So if we have no border-crossing trips, here, the plan only consists of inner trips.
//						if we have only inner trips, we do not want to create ptOnly plan because it will be the same as a pt plan (when pt is configured as replacing mode) and thus increase the chance of choosing pt naturally.
						plansToAdd.add(createPTOnlyPlan(plan, enforceMassConservation, mainModeIdentifier, fac));
					}
				}

				plan.setType(replacingMode);

				PRStation firstCarPRStation = null;
				//we use this as 'iteration variable'
				PRStation currentCarPRStation = null;

				for (TripStructureUtils.Trip trip : tripsToReplace) {
					TripType tripType = (TripType) trip.getTripAttributes().getAttribute(TRIP_TYPE_ATTR_KEY);
					String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
					if (tripType.equals(TripType.outsideTrip)){
						if(mainMode.equals(TransportMode.car) && currentCarPRStation != null) throw new IllegalStateException("agent " + person + "performs an outside trip from " + trip.getOriginActivity() + "to " + trip.getDestinationActivity() +
								"\n but vehicle is still parked at link=" + currentCarPRStation);
						continue;
					}

					List<PlanElement> newTrip;
					PRStation prStation = null;

					if(tripType.equals(TripType.innerTrip)) {
						Leg l1 = fac.createLeg(replacingMode);
						TripStructureUtils.setRoutingMode(l1, replacingMode);
						l1.getAttributes().putAttribute("replacing", mainMode); //is important to filter the leg later when replacing with other modes!
						newTrip = List.of(l1);
					} else if(tripType.equals(TripType.originatingTrip)) {
						if(mainMode.equals(TransportMode.car)){
							nrOfBorderCrossingCarTrips --;
							//some consistency (mass conservation) checks
							if(nrOfBorderCrossingCarTrips == 0 && livesInProhibitionZone){
								throw new IllegalStateException("agent " + person.getId() + "lives inside but travels outside the border with car without returning with a prohibited mode.\n" +
										"trip = " + trip);
							}
							//car has to be picked up where it was left the last time
							if (currentCarPRStation != null && enforceMassConservation){
								prStation = currentCarPRStation;
							}
						}
					 	if(prStation == null){ //if no car trip into zone was observed before or if the mode is ride, we enter here
							Activity act = prStationChoice.equals(PRStationChoice.closestToInsideActivity) ? trip.getOriginActivity() : trip.getDestinationActivity();
							List<PRStation> prStationCandidates = straightLineKnnFinder.findNearest(act, prStations.stream());
							List<PRStation> mutableListCandidates = new ArrayList<>(prStationCandidates);
							Collections.shuffle(mutableListCandidates);
							int rndr = rnd.nextInt(mutableListCandidates.size());
							prStation = mutableListCandidates.get(rndr);
						}

						 //change value of firstCarPRStation only one time
						firstCarPRStation = firstCarPRStation == null ? prStation : firstCarPRStation;
						currentCarPRStation = null;

						Activity parkAndRideAct = fac.createActivityFromCoord(PR_ACTIVITY_TYPE, prStation.getCoord());
						parkAndRideAct.setMaximumDuration(5 * 60);
						parkAndRideAct.setLinkId(prStation.linkId);

						newTrip = new ArrayList<>();
						Leg l1 = fac.createLeg(replacingMode);
						TripStructureUtils.setRoutingMode(l1, replacingMode);
						l1.getAttributes().putAttribute("replacing", mainMode); //is important to filter the leg later when replacing with other modes!
						Leg l2 = fac.createLeg(mainMode);
						TripStructureUtils.setRoutingMode(l2, mainMode);

						newTrip.add(l1); //new mode
						newTrip.add(parkAndRideAct);
						newTrip.add(l2); //old main mode

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
								if(enforceMassConservation){
									prStation = firstCarPRStation;
								}
							}
						}
					 	if(prStation == null) { //if not the last border-crossing car or a ride trip
							Activity act = prStationChoice.equals(PRStationChoice.closestToInsideActivity) ? trip.getDestinationActivity() : trip.getOriginActivity();
							List<PRStation> prStationCandidates = straightLineKnnFinder.findNearest(act, prStations.stream());
							List<PRStation> mutableListCandidates = new ArrayList<>(prStationCandidates);
							Collections.shuffle(mutableListCandidates);
							int rndr = rnd.nextInt(mutableListCandidates.size());
							prStation = mutableListCandidates.get(rndr);
							if(mainMode.equals(TransportMode.car)){
								currentCarPRStation = prStation;
							}
						}

//						Activity parkAndRideAct = fac.createActivityFromLinkId(PR_ACTIVITY_TYPE, prStation);
						Activity parkAndRideAct = fac.createActivityFromCoord(PR_ACTIVITY_TYPE, prStation.getCoord());
						parkAndRideAct.setMaximumDuration(5 * 60);
						parkAndRideAct.setLinkId(prStation.linkId);

						newTrip = new ArrayList<>();

						Leg l1 = fac.createLeg(mainMode);
						TripStructureUtils.setRoutingMode(l1, mainMode);
						Leg l2 = fac.createLeg(replacingMode);
						TripStructureUtils.setRoutingMode(l2, replacingMode);
						l2.getAttributes().putAttribute("replacing", mainMode); //is important to filter the leg later when replacing with other modes!

						newTrip.add(l1); //old main mode
						newTrip.add(parkAndRideAct);
						newTrip.add(l2); //new mode

					} else {
						throw new IllegalArgumentException("unknown trip type: " + tripType);
					}

					//insert new trip into plan
					TripRouter.insertTrip(plan.getPlanElements(), trip.getOriginActivity(), newTrip, trip.getDestinationActivity());
					replacedTrips.increment();
				}

				//for all other replacing modes, we want to apply the same logic. so we can basically copy the plan and just override the leg modes.
				while (replacingModeIterator.hasNext()){
					String otherReplacingMode = replacingModeIterator.next();
					Plan planCopy = fac.createPlan();
					planCopy.setPerson(person);
					PopulationUtils.copyFromTo(plan, planCopy); //important to copy first and than set the type, because in the copy method the type is included for copying...
					planCopy.setType(otherReplacingMode);

					//override leg modes
					TripStructureUtils.getLegs(planCopy).stream()
							.filter(leg -> leg.getAttributes().getAttribute("replacing") != null) //we have marked replacing legs. not the best solution but works. otherwise we would have to apply the entire logic of this method to all plan copies separately.
							.forEach(leg -> leg.setMode(otherReplacingMode));
					plansToAdd.add(planCopy);
				}

			}
			//after we've iterated over existing plans, add all the plan copies
			plansToAdd.forEach(plan -> person.addPlan(plan));
		}
		//TODO iterate over all plans, set score to null !!


		log.info("overall nr of trips replaced = " + replacedTrips);
		log.info("finished modifying input plans....");
	}

	private static Plan createPTOnlyPlan(Plan originalPlan, boolean enforceMassConservation, MainModeIdentifier mainModeIdentifier, PopulationFactory fac) {
		Plan planCopy = fac.createPlan();
		planCopy.setPerson(originalPlan.getPerson());
		PopulationUtils.copyFromTo(originalPlan, planCopy); //important to copy first and than set the type, because in the copy method the type is included for copying...
		planCopy.setType("ptOnly");


		if(enforceMassConservation){
			//beware: it might be that the person has multiple subtours and we only need to replace some of them.
			// for example (home -> (work -> leisure -> work) -> home). with home being inside the prohibition zone and every other activity being outside
			//even a bit more complicated: an agent might have (home -> (work -> leisure -> work) - other -> home)
			//where, again, only home is inside the prohibition zone.
			// In both cases, we can keep car for the work-leisure (so the inner) subtour, but not for the other part of the outer subtour (containing the inner one)
			for(TripStructureUtils.Subtour subtour : TripStructureUtils.getSubtours(planCopy)){

				//if we find any trip that touches the prohibition zone within the exclusive trips of this subtour, we need to take action
				boolean subtourTouchesProhibitionZoneWithCarOrRide = subtour.getTripsWithoutSubSubtours().stream()
						.filter(trip -> trip.getTripAttributes().getAttribute(TRIP_TYPE_ATTR_KEY) != null && //if the attribute was never set, the trip is neither with car nor ride
								!trip.getTripAttributes().getAttribute(TRIP_TYPE_ATTR_KEY).equals(TripType.outsideTrip))
						.findAny().isPresent();

				if(subtourTouchesProhibitionZoneWithCarOrRide){
					log.warn("assuming (with hardcoding) that ride is considered as non-chain-based and car is chain-based");
					//if car is used, change all trips to pt
					if(subtour.getTripsWithoutSubSubtours().stream()
							.filter(trip -> mainModeIdentifier.identifyMainMode(trip.getTripElements()).equals(TransportMode.car))
							.findAny().isPresent()){
						replaceTripsWithPtTrips(subtour.getTripsWithoutSubSubtours(), fac, planCopy);
					} else {
						//replace only the ride trips that touch the prohibition zone - with pt trips
						List<TripStructureUtils.Trip> tripsToReplace = subtour.getTripsWithoutSubSubtours().stream()
								.filter(trip -> (mainModeIdentifier.identifyMainMode(trip.getTripElements()).equals(TransportMode.ride) &&
												! trip.getTripAttributes().getAttribute(TRIP_TYPE_ATTR_KEY).equals(TripType.outsideTrip))
										)
								.collect(Collectors.toList());
						replaceTripsWithPtTrips(tripsToReplace, fac, planCopy);
					}
				}
			}
		} else {
			//we can just replace any trip that touches the prohibition zone with pt and do not have to take care of outer subtours
			List<TripStructureUtils.Trip> tripsToReplace = TripStructureUtils.getTrips(planCopy).stream()
					.filter(trip -> (trip.getTripAttributes().getAttribute(TRIP_TYPE_ATTR_KEY) != null) &&
							(! trip.getTripAttributes().getAttribute(TRIP_TYPE_ATTR_KEY).equals(TripType.outsideTrip)) )
					.collect(Collectors.toList());
			replaceTripsWithPtTrips(tripsToReplace, fac, planCopy);
		}

		return planCopy;
	}

	private static void replaceTripsWithPtTrips(List<TripStructureUtils.Trip> trips, PopulationFactory fac, Plan planCopy) {
		for (TripStructureUtils.Trip trip : trips) {
				//overwrite trip mode
				Leg leg = fac.createLeg(TransportMode.pt);
				TripStructureUtils.setRoutingMode(leg, TransportMode.pt);
				TripRouter.insertTrip(planCopy, trip.getOriginActivity(), List.of(leg), trip.getDestinationActivity());
		}
	}

	/**
	 *
	 * @param url2PRStations a .tsv input file with the following columns (and a header row): 'name', 'x', 'y' and 'linkId'. The order should not matter.
	 * @return
	 */
	static Set<PRStation> readPRStationFile(URL url2PRStations) {
		log.info("read input file for P+R stations");
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

class PRStation {

	String name;
	Id<Link> linkId;
	Coord coord;

	PRStation(String name, Id<Link> linkId, Coord coord){
		this.name = name;
		this.linkId = linkId;
		this.coord = coord;
	}

	public Coord getCoord() {
		return coord;
	}
}
