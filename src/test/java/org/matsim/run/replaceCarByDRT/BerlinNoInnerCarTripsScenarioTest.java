package org.matsim.run.replaceCarByDRT;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.drt.OpenBerlinIntermodalPtDrtRouterModeIdentifier;

import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.run.replaceCarByDRT.ReplaceCarByDRT.PR_ACTIVITY_TYPE;
import static org.matsim.run.replaceCarByDRT.ReplaceCarByDRT.readPRStationFile;
import static org.matsim.run.replaceCarByDRT.RunBerlinNoInnerCarTripsScenario.URL_2_PR_STATIONS;


//TODO write a copy of this test class, set up simulation with PRStationChoice.closestToInside, check for PR activity locations

/**
 * These test are meant to work with the following configuration:
 * PRStationChoice = closestToOutside
 * replacingModes = pt+drt
 * extraPtPlan = true
 */
public class BerlinNoInnerCarTripsScenarioTest {

	private static Logger log = LogManager.getLogger(BerlinNoInnerCarTripsScenarioTest.class);
	private static Map<String, PRStation> PR_STATIONS = readPRStationFile(URL_2_PR_STATIONS).stream().collect(Collectors.toMap(station -> station.getName(), station -> station));
	private static Scenario SCENARIO_CLOSEST_INSIDE;


	@BeforeClass
	public static void main() {

		SCENARIO_CLOSEST_INSIDE = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationUtils.readPopulation(SCENARIO_CLOSEST_INSIDE.getPopulation(), "scenarios/berlin/replaceCarByDRT/noModeChoice/replaceCarByDRT.testPlans.xml.gz");
		ReplaceCarByDRT.prepareInputPlansForCarProhibitionWithPRLogic(SCENARIO_CLOSEST_INSIDE,
				Set.of(TransportMode.drt, TransportMode.pt),
				IOUtils.resolveFileOrResource("scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp"),
				URL_2_PR_STATIONS,
				new OpenBerlinIntermodalPtDrtRouterModeIdentifier(),
				ReplaceCarByDRT.PRStationChoice.closestToInsideActivity,
				true,
				true,
				1
				);

	}


	@Test
	public void testAgentAttributes(){
		Set<String> violations = new HashSet<>();

		for (Person person : SCENARIO_CLOSEST_INSIDE.getPopulation().getPersons().values()) {
			if (!PopulationUtils.getSubpopulation(person).equals("person")) continue;

			if (person.getAttributes().getAttribute("livesInProhibitionZone") == null) {
				violations.add("person " + person.getId() + "does not have livesInProhibitionZone attribute");
			}
		}

		if(! violations.isEmpty()){
			String violationsString = "";
			for (String violation : violations) {
				violationsString += "\n" + violation;
			}
			log.error("There were the following violations in the plans:" + violationsString);
		}
		Assert.assertEquals("There were violations in the agent plans. See above.",
				0,
				violations.size());
	}

	@Test
	public void testBerlinerWithExternalTripsOnly(){
		// this agent lives outside the prohibition zone & has external trips -> should not be affected.

		Person person = SCENARIO_CLOSEST_INSIDE.getPopulation().getPersons().get(Id.createPersonId(340500501));

		Assert.assertEquals("berlin", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));

		//test all plans
		for (Plan plan : person.getPlans()) {
			Assert.assertEquals("person " + person.getId() + " should not be affected by prohibition zone", "not-affected", plan.getType());
			List<Activity> prActs = getPRActivities(plan);
			Assert.assertEquals(0, prActs.size()); //nr of PR acts
		}
	}

	@Test
	public void testBrandenburgerWith2PRActivities(){
		// this agent lives in Brandenburg, enters the prohibition zone once -> has 2 PRActivities at the same PRStation
		Person person = SCENARIO_CLOSEST_INSIDE.getPopulation().getPersons().get(Id.createPersonId(38250801));
		Assert.assertEquals("brandenburg", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
		Assert.assertEquals(false, person.getAttributes().getAttribute("livesInProhibitionZone"));

		//test all plans
		Assert.assertEquals("person " + person.getId() + " should have 3 plans (with types [drt,pt,ptOnly])", 3, person.getPlans().size());

		for (Plan plan : person.getPlans()) {
			List<Activity> prActs = getPRActivities(plan);
			if(!plan.getType().equals("ptOnly")){
				Assert.assertEquals(2, prActs.size()); //nr of PR acts
				Assert.assertEquals(PR_STATIONS.get("Innsbrucker").linkId,prActs.get(0).getLinkId());
				Assert.assertEquals(PR_STATIONS.get("Innsbrucker").coord,prActs.get(0).getCoord());
			} else {
				Assert.assertEquals(0, prActs.size()); //nr of PR acts
			}
		}

//		if(testCaseToggle.equals(ReplaceCarByDRT.PRStationChoice.closestToOutSideActivity)){
//			Assert.assertEquals(PR_STATIONS.get("Bundesplatz").linkId,personsPRends.get(0).getLinkId());
//			Assert.assertEquals(PR_STATIONS.get("Bundesplatz").coord,personsPRends.get(0).getCoord());
//			Assert.assertEquals(PR_STATIONS.get("Bundesplatz").linkId,personsPRends.get(1).getLinkId());
//			Assert.assertEquals(PR_STATIONS.get("Bundesplatz").coord,personsPRends.get(1).getCoord());
//		} else{
//			// prStationChoice = closestToInside
//			Assert.assertEquals(PR_STATIONS.get("Innsbrucker").linkId,personsPRends.get(0).getLinkId());
//			Assert.assertEquals(PR_STATIONS.get("Innsbrucker").coord,personsPRends.get(0).getCoord());
//			Assert.assertEquals(PR_STATIONS.get("Innsbrucker").linkId,personsPRends.get(1).getLinkId());
//			Assert.assertEquals(PR_STATIONS.get("Innsbrucker").coord,personsPRends.get(1).getCoord());
//		}

	}

//	@Test
//	public void testBrandenburgerWith4PRActivities(){
//		// this agent (lives outside prohibition zone) and has multiple (4) border-crossing trips -> 4x PRActivities. First and last PR station can be different!
//
//		Person person = SCENARIO_CLOSEST_INSIDE.getPopulation().getPersons().get(Id.createPersonId(116685501));
//
//		Assert.assertEquals("brandenburg", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
//		Assert.assertEquals(false, person.getAttributes().getAttribute("livesInProhibitionZone"));
//
//		//test all plans
//		for (Plan plan : person.getPlans()) {
//			if(!plan.getType().equals("ptOnly")){
//				getPRActivities(plan, 4);
//			} else {
//				getPRActivities(plan, 0);
//			}
//		}
//
//		//test events
//		assertPRActivityEndEventsCountToX(person, 4);
//		List<ActivityEndEvent> personsPRends = PR_HANDLER.getPrActivityEndEvents().stream()
//				.filter(event -> event.getPersonId().equals(person.getId()))
//				.collect(Collectors.toList());
//
//		//identical for closestToOutside & closestToInside
//		Assert.assertEquals(PR_STATIONS.get("Gesundbrunnen").linkId,personsPRends.get(0).getLinkId());
//		Assert.assertEquals(PR_STATIONS.get("Gesundbrunnen").coord,personsPRends.get(0).getCoord());
//		Assert.assertEquals(PR_STATIONS.get("Gesundbrunnen").linkId,personsPRends.get(1).getLinkId());
//		Assert.assertEquals(PR_STATIONS.get("Gesundbrunnen").coord,personsPRends.get(1).getCoord());
//		Assert.assertEquals(PR_STATIONS.get("Wedding").linkId,personsPRends.get(2).getLinkId());
//		Assert.assertEquals(PR_STATIONS.get("Wedding").coord,personsPRends.get(2).getCoord());
//		Assert.assertEquals(PR_STATIONS.get("Wedding").linkId,personsPRends.get(3).getLinkId());
//		Assert.assertEquals(PR_STATIONS.get("Wedding").coord,personsPRends.get(3).getCoord());
//
//	}
//
//	@Test
//	public void testInhabitantOfProhibitionZoneWithMultipleBorderCrossings(){
//		//this agent has multiple trips across the border of prohibition zone (at best 4 or more - possibly at different PR stations). Testing that firstPRStation = lastPRStation
//
//		Person person = SCENARIO_CLOSEST_INSIDE.getPopulation().getPersons().get(Id.createPersonId(187328501));
//		Assert.assertEquals("expecting agent's home-activity-zone to be berlin", "berlin", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
//		Assert.assertEquals(true, person.getAttributes().getAttribute("livesInProhibitionZone"));
//
//		//test all plans
//		for (Plan plan : person.getPlans()) {
//			if(!plan.getType().equals("ptOnly")){
//				getPRActivities(plan, 4);
//			} else {
//				getPRActivities(plan, 0);
//			}
//		}
//
//		//test events
//		assertPRActivityEndEventsCountToX(person, 4);
//		List<ActivityEndEvent> personsPRends = PR_HANDLER.getPrActivityEndEvents().stream()
//				.filter(event -> event.getPersonId().equals(person.getId()))
//				.collect(Collectors.toList());
//
//		//identical for closestToOutside & closestToInside
//		Assert.assertEquals(PR_STATIONS.get("Jungfernheide").linkId,personsPRends.get(0).getLinkId());
//		Assert.assertEquals(PR_STATIONS.get("Jungfernheide").coord,personsPRends.get(0).getCoord());
//		Assert.assertEquals(PR_STATIONS.get("Beusselstrasse").linkId,personsPRends.get(1).getLinkId());
//		Assert.assertEquals(PR_STATIONS.get("Beusselstrasse").coord,personsPRends.get(1).getCoord());
//		Assert.assertEquals(PR_STATIONS.get("Beusselstrasse").linkId,personsPRends.get(2).getLinkId());
//		Assert.assertEquals(PR_STATIONS.get("Beusselstrasse").coord,personsPRends.get(2).getCoord());
//		Assert.assertEquals(PR_STATIONS.get("Jungfernheide").linkId,personsPRends.get(3).getLinkId());
//		Assert.assertEquals(PR_STATIONS.get("Jungfernheide").coord,personsPRends.get(3).getCoord());
//
//	}
//
//	@Test
//	public void testInhabitantOfProhibitionZoneWithOutSideCarSubtour(){
//		// this agent lives in the prohibition zone, travels to work outside,
//		// than has a subtour outside that could normally be a car subtour, but unfortunately the coordinates of both work differ slightly
//		// meaning that the ptOnly plan replaces ALL trips by pt trips, inclduing the subtour that could remain car!
//		// for not ptOnly-plan: testing firstCarPRStation == lastCarPRStation
//		Person person = SCENARIO_CLOSEST_INSIDE.getPopulation().getPersons().get(Id.createPersonId(259563501));
//		Assert.assertEquals("expecting agent's home-activity-zone to be berlin", "berlin", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
//		Assert.assertEquals(true, person.getAttributes().getAttribute("livesInProhibitionZone"));
//
//		for (Plan plan : person.getPlans()) {
//			if(!plan.getType().equals("ptOnly")){
//				getPRActivities(plan, 2);
//			} else {
//				getPRActivities(plan, 0);
//			}
//		}
//
//		//test events
//		assertPRActivityEndEventsCountToX(person, 2);
//
//	}
//
//	@Test
//	public void testInhabitantOfProhibitionZoneWithOutsideANDCrossingSubtours(){
//		//this agent lives in berlin outside the prohibiton zone
//		//.. has one outside subtour and one border-crossing subtour, both start+end @ home
//		Person person = SCENARIO_CLOSEST_INSIDE.getPopulation().getPersons().get(Id.createPersonId(446367901));
//
//		Assert.assertEquals("berlin", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
//		Assert.assertEquals(false, person.getAttributes().getAttribute("livesInProhibitionZone"));
//
//		//test all plans
//		for (Plan plan : person.getPlans()) {
//			if(!plan.getType().equals("ptOnly")){
//				getPRActivities(plan, 2);
//			} else {
//				getPRActivities(plan, 0);
//				Collection<TripStructureUtils.Subtour> subtours = TripStructureUtils.getSubtours(plan);
//				Assert.assertEquals("there should be exactly one subtour that contains legs with routing mode car",
//						1,
//						subtours.stream()
//						.filter(subtour -> subtour.getTripsWithoutSubSubtours().stream()
//								.flatMap(trip -> trip.getLegsOnly().stream())
//								.filter(leg -> TripStructureUtils.getRoutingMode(leg).equals(TransportMode.car))
//								.findAny().isPresent())
//						.count());
//				Assert.assertEquals("there should be exactly one subtour that contains legs with routing mode pt",
//						1,
//						subtours.stream()
//								.filter(subtour -> subtour.getTripsWithoutSubSubtours().stream()
//										.flatMap(trip -> trip.getLegsOnly().stream())
//										.filter(leg -> TripStructureUtils.getRoutingMode(leg).equals(TransportMode.pt))
//										.findAny().isPresent())
//								.count());
//				Assert.assertEquals(2, subtours.size());
//			}
//		}
//
//		//test events
//		assertPRActivityEndEventsCountToX(person, 2);
//	}
//
//	@Test
//	public void testBerlinerWithInnerTripsOnly(){
//
//		Person person = SCENARIO_CLOSEST_INSIDE.getPopulation().getPersons().get(Id.createPersonId(272337601));
//
//		Assert.assertEquals("berlin", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
//		Assert.assertEquals(true, person.getAttributes().getAttribute("livesInProhibitionZone"));
//
//		//test all plans
//		//agent has only inner trips
//		for (Plan plan : person.getPlans()) {
//			getPRActivities(plan, 0);
//		}
//
//		//test events
//		assertPRActivityEndEventsCountToX(person, 0);
//	}
//
//
//
//	@Test
//	public void testRiderWithDifferentPRStations(){
//		// test agent with mode ride that uses different PR stations (as this is not subject to mass conservation)
//
//		Person person = SCENARIO_CLOSEST_INSIDE.getPopulation().getPersons().get(Id.createPersonId(116317401));
//
//		//test all plans
//		for (Plan plan : person.getPlans()) {
//			if(!plan.getType().equals("ptOnly")){
//				getPRActivities(plan, 2);
//			} else {
//				getPRActivities(plan, 0);
//			}
//		}
//
//		//test events
//		assertPRActivityEndEventsCountToX(person, 2);
//		List<ActivityEndEvent> personsPRends = PR_HANDLER.getPrActivityEndEvents().stream()
//				.filter(event -> event.getPersonId().equals(person.getId()))
//				.collect(Collectors.toList());
//
//		if(testCaseToggle.equals(ReplaceCarByDRT.PRStationChoice.closestToOutSideActivity)){
//			Assert.assertEquals(PR_STATIONS.get("Westkreuz/ZOB").linkId,personsPRends.get(0).getLinkId());
//			Assert.assertEquals(PR_STATIONS.get("Westkreuz/ZOB").coord,personsPRends.get(0).getCoord());
//			Assert.assertEquals(PR_STATIONS.get("Gesundbrunnen").linkId,personsPRends.get(1).getLinkId());
//			Assert.assertEquals(PR_STATIONS.get("Gesundbrunnen").coord,personsPRends.get(1).getCoord());
//
//		} else{
//			// prStationChoice = closestToInside, here it´s actually same PRStation. TODO: find better agent to test?
//			Assert.assertEquals(PR_STATIONS.get("Westkreuz/ZOB").linkId,personsPRends.get(0).getLinkId());
//			Assert.assertEquals(PR_STATIONS.get("Westkreuz/ZOB").coord,personsPRends.get(0).getCoord());
//			Assert.assertEquals(PR_STATIONS.get("Westkreuz/ZOB").linkId,personsPRends.get(1).getLinkId());
//			Assert.assertEquals(PR_STATIONS.get("Westkreuz/ZOB").coord,personsPRends.get(1).getCoord());
//		}
//	}
//
//
//	@Test
//	public void testRiderWith2BorderCrossingTripsANDDifferentModes(){
//		// this agent is a rider and has two border crossing trips. The one is mode "ride", the other mode "pt" -> should contain 1 PRActivity
//
//		Person person = SCENARIO_CLOSEST_INSIDE.getPopulation().getPersons().get(Id.createPersonId(100370701));
//
//		//test all plans
//		for (Plan plan : person.getPlans()) {
//			if(!plan.getType().equals("ptOnly")){
//				getPRActivities(plan, 1);
//			} else {
//				getPRActivities(plan, 0);
//			}
//		}
//
//		//test events
//		assertPRActivityEndEventsCountToX(person, 1);
//
//	}


	private static List<Activity> getPRActivities(Plan plan) {
		List<Activity> PRacts = new ArrayList<>();
		for(Activity activity: PopulationUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.StagesAsNormalActivities)){
			if(activity.getType().equals(PR_ACTIVITY_TYPE)) PRacts.add(activity);
		}
		return PRacts;
	}

}