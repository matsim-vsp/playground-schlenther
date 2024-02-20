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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.drt.OpenBerlinIntermodalPtDrtRouterModeIdentifier;

import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.run.replaceCarByDRT.PRStation.readPRStationFile;
import static org.matsim.run.replaceCarByDRT.ReplaceCarByDRT.PR_ACTIVITY_TYPE;

/**
 * These tests are meant to work with the following configuration:
 * PRStationChoice = closestToOutside / closestToInside
 * replacingModes = pt+drt
 * extraPtPlan = true
 */
public class ReplaceCarByDRTTest {

	private static Logger log = LogManager.getLogger(ReplaceCarByDRTTest.class);
	private static Map<String, PRStation> PR_STATIONS = readPRStationFile(IOUtils.resolveFileOrResource("scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-07-27-pr-stations.tsv"))
			.stream().collect(Collectors.toMap(station -> station.getName(), station -> station));
	private static Scenario SCENARIO;

	@BeforeClass
	public static void main() {
		SCENARIO = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationUtils.readPopulation(SCENARIO.getPopulation(),"scenarios/berlin/replaceCarByDRT/noModeChoice/replaceCarByDRT.testPlans.xml.gz");
		NetworkUtils.readNetwork(SCENARIO.getNetwork(),
				"https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz");

		ReplaceCarByDRT.prepareInputPlansForCarProhibitionWithPRLogic(SCENARIO,
				Set.of(TransportMode.car, TransportMode.ride),
				Set.of(TransportMode.drt, TransportMode.pt),
				IOUtils.resolveFileOrResource("scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp"),
				IOUtils.resolveFileOrResource("scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-07-27-pr-stations.tsv"),
				new OpenBerlinIntermodalPtDrtRouterModeIdentifier(),
				//always choose the closest P+R station for this test
				1
		);
	}

	@Test
	public void testBanCarAndRideFromNetworkArea(){
		ReplaceCarByDRT.banCarAndRideFromNetworkArea(SCENARIO,
				IOUtils.resolveFileOrResource("scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp"),
				Set.of("motorway"));

		//checks 1 car route that did not touch one forbidden link and still has its route
		// && checks 1 car route that touched one forbidden link and now the route is null - Agent: 33963001, Car-Leg: 3 (between leisure_3000 & leisure_3600)
		Person person = SCENARIO.getPopulation().getPersons().get(Id.createPersonId(33963001));
		Plan plan = person.getSelectedPlan();
		int i = 0;
		for (Leg leg : TripStructureUtils.getLegs(plan)) {
			if(leg.getMode().equals(TransportMode.car)) {
				i++;
				if(i == 1) {
					Assert.assertNotEquals(null, leg.getRoute());
				}
				if(i == 3) {
					Assert.assertEquals(null, leg.getRoute());
				}
			}
		}

		//motorway links inside ban area still allow cars
		Assert.assertTrue(SCENARIO.getNetwork().getLinks().get(Id.createLinkId("47278")).getAllowedModes().contains(TransportMode.car));

		//secondary links inside ban area do not allow cars anymore
		Assert.assertFalse(SCENARIO.getNetwork().getLinks().get(Id.createLinkId("145687")).getAllowedModes().contains(TransportMode.car));

		//secondary links outside ban area still allow cars
		Assert.assertTrue(SCENARIO.getNetwork().getLinks().get(Id.createLinkId("66263")).getAllowedModes().contains(TransportMode.car));

	}


	@Test
	public void testAgentAttributes(){
		Set<String> violations = new HashSet<>();

		for (Person person : SCENARIO.getPopulation().getPersons().values()) {
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

		Person person = SCENARIO.getPopulation().getPersons().get(Id.createPersonId(340500501));

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

		Person person = SCENARIO.getPopulation().getPersons().get(Id.createPersonId(38250801));
		Assert.assertEquals("brandenburg", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
		Assert.assertEquals(false, person.getAttributes().getAttribute("livesInProhibitionZone"));

		//test all plans
		Assert.assertEquals("person " + person.getId() + " should have 5 plans (with types [2x drt,2x pt,ptOnly])", 5, person.getPlans().size());

		for (Plan plan : person.getPlans()) {
			List<Activity> prActs = getPRActivities(plan);
			if(!plan.getType().equals("ptOnly")){
				Assert.assertEquals(2, prActs.size()); //nr of PR acts

				if(plan.getType().contains("closestToInside")) {
					// prStationChoice = closestToInside
					Assert.assertEquals(PR_STATIONS.get("Innsbrucker").getLinkId(),prActs.get(0).getLinkId());
					Assert.assertEquals(PR_STATIONS.get("Innsbrucker").getCoord(),prActs.get(0).getCoord());
					Assert.assertEquals(PR_STATIONS.get("Innsbrucker").getLinkId(),prActs.get(1).getLinkId());
					Assert.assertEquals(PR_STATIONS.get("Innsbrucker").getCoord(),prActs.get(1).getCoord());
				} else {
					// prStationChoice = closestToOutside
					Assert.assertEquals(PR_STATIONS.get("Bundesplatz").getLinkId(),prActs.get(0).getLinkId());
					Assert.assertEquals(PR_STATIONS.get("Bundesplatz").getCoord(),prActs.get(0).getCoord());
					Assert.assertEquals(PR_STATIONS.get("Bundesplatz").getLinkId(),prActs.get(1).getLinkId());
					Assert.assertEquals(PR_STATIONS.get("Bundesplatz").getCoord(),prActs.get(1).getCoord());
				}


			} else {
				Assert.assertEquals(0, prActs.size()); //nr of PR acts
			}
		}
	}

	@Test
	public void testBrandenburgerWith4PRActivities(){
		// this agent (lives outside prohibition zone) and has multiple (4) border-crossing trips -> 4x PRActivities.
		// First and last PR station can be different!

		Person person = SCENARIO.getPopulation().getPersons().get(Id.createPersonId(116685501));

		Assert.assertEquals("brandenburg", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
		Assert.assertEquals(false, person.getAttributes().getAttribute("livesInProhibitionZone"));

		//test all plans
		Assert.assertEquals("person " + person.getId() + " should have 5 plans (with types [2x drt,2x pt,ptOnly])", 5, person.getPlans().size());

		for (Plan plan : person.getPlans()) {
			List<Activity> prActs = getPRActivities(plan);
			if(!plan.getType().equals("ptOnly")){
				Assert.assertEquals(4, prActs.size()); //nr of PR acts

				Assert.assertEquals(PR_STATIONS.get("Gesundbrunnen").getLinkId(),prActs.get(0).getLinkId());
				Assert.assertEquals(PR_STATIONS.get("Gesundbrunnen").getCoord(),prActs.get(0).getCoord());
				Assert.assertEquals(PR_STATIONS.get("Gesundbrunnen").getLinkId(),prActs.get(1).getLinkId());
				Assert.assertEquals(PR_STATIONS.get("Gesundbrunnen").getCoord(),prActs.get(1).getCoord());
				Assert.assertEquals(PR_STATIONS.get("Wedding").getLinkId(),prActs.get(2).getLinkId());
				Assert.assertEquals(PR_STATIONS.get("Wedding").getCoord(),prActs.get(2).getCoord());
				Assert.assertEquals(PR_STATIONS.get("Wedding").getLinkId(),prActs.get(3).getLinkId());
				Assert.assertEquals(PR_STATIONS.get("Wedding").getCoord(),prActs.get(3).getCoord());

			} else {
				Assert.assertEquals(0, prActs.size()); //nr of PR acts
			}
		}
	}

	@Test
	public void testInhabitantOfProhibitionZoneWithMultipleBorderCrossings(){
		//this agent has multiple trips across the border of prohibition zone (at best 4 or more - possibly at different PR stations).
		//Testing that firstPRStation = lastPRStation

		Person person = SCENARIO.getPopulation().getPersons().get(Id.createPersonId(187328501));
		Assert.assertEquals("expecting agent's home-activity-zone to be berlin", "berlin", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
		Assert.assertEquals(true, person.getAttributes().getAttribute("livesInProhibitionZone"));

		//test all plans
		Assert.assertEquals("person " + person.getId() + " should have 5 plans (with types [2x drt,2x pt,ptOnly])", 5, person.getPlans().size());

		for (Plan plan : person.getPlans()) {
			List<Activity> prActs = getPRActivities(plan);
			if(!plan.getType().equals("ptOnly")){
				Assert.assertEquals(4, prActs.size()); //nr of PR acts

				Assert.assertEquals(PR_STATIONS.get("Jungfernheide").getLinkId(),prActs.get(0).getLinkId());
				Assert.assertEquals(PR_STATIONS.get("Jungfernheide").getCoord(),prActs.get(0).getCoord());
				Assert.assertEquals(PR_STATIONS.get("Beusselstrasse").getLinkId(),prActs.get(1).getLinkId());
				Assert.assertEquals(PR_STATIONS.get("Beusselstrasse").getCoord(),prActs.get(1).getCoord());
				Assert.assertEquals(PR_STATIONS.get("Beusselstrasse").getLinkId(),prActs.get(2).getLinkId());
				Assert.assertEquals(PR_STATIONS.get("Beusselstrasse").getCoord(),prActs.get(2).getCoord());
				Assert.assertEquals(PR_STATIONS.get("Jungfernheide").getLinkId(),prActs.get(3).getLinkId());
				Assert.assertEquals(PR_STATIONS.get("Jungfernheide").getCoord(),prActs.get(3).getCoord());

			} else {
				Assert.assertEquals(0, prActs.size()); //nr of PR acts
			}
		}
	}

	@Test
	public void testInhabitantOfProhibitionZoneWithOutSideCarSubtour(){
		// this agent lives in the prohibition zone, travels to work outside,
		// than has a subtour outside that could normally be a car subtour, but unfortunately the coordinates of both work differ slightly
		// meaning that the ptOnly plan replaces ALL trips by pt trips, inclduing the subtour that could remain car!


		Person person = SCENARIO.getPopulation().getPersons().get(Id.createPersonId(259563501));
		Assert.assertEquals("expecting agent's home-activity-zone to be berlin", "berlin", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
		Assert.assertEquals(true, person.getAttributes().getAttribute("livesInProhibitionZone"));

		//test all plans
		Assert.assertEquals("person " + person.getId() + " should have 5 plans (with types [2x drt,2x pt,ptOnly])", 5, person.getPlans().size());

		for (Plan plan : person.getPlans()) {
			List<Activity> prActs = getPRActivities(plan);
			if(!plan.getType().equals("ptOnly")){
				Assert.assertEquals(2, prActs.size()); //nr of PR acts

			} else {
				Assert.assertEquals(0, prActs.size()); //nr of PR acts
			}
		}

	}

	@Test
	public void testInhabitantOfProhibitionZoneWithOutsideANDCrossingSubtours(){
		//this agent lives in berlin outside the prohibiton zone
		//it has one outside subtour and one border-crossing subtour, both start+end @ home

		Person person = SCENARIO.getPopulation().getPersons().get(Id.createPersonId(446367901));

		Assert.assertEquals("berlin", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
		Assert.assertEquals(false, person.getAttributes().getAttribute("livesInProhibitionZone"));

		//test all plans
		Assert.assertEquals("person " + person.getId() + " should have 5 plans (with types [2x drt,2x pt,ptOnly])", 5, person.getPlans().size());

		for (Plan plan : person.getPlans()) {
			List<Activity> prActs = getPRActivities(plan);
			if(!plan.getType().equals("ptOnly")){
				Assert.assertEquals(2, prActs.size()); //nr of PR acts
			} else {
				Assert.assertEquals(0, prActs.size()); //nr of PR acts
				Collection<TripStructureUtils.Subtour> subtours = TripStructureUtils.getSubtours(plan);
				Assert.assertEquals("there should be exactly one subtour that contains legs with routing mode car",
						1,
						subtours.stream()
						.filter(subtour -> subtour.getTripsWithoutSubSubtours().stream()
								.flatMap(trip -> trip.getLegsOnly().stream())
								.filter(leg -> TripStructureUtils.getRoutingMode(leg).equals(TransportMode.car))
								.findAny().isPresent())
						.count());
				Assert.assertEquals("there should be exactly one subtour that contains legs with routing mode pt",
						1,
						subtours.stream()
								.filter(subtour -> subtour.getTripsWithoutSubSubtours().stream()
										.flatMap(trip -> trip.getLegsOnly().stream())
										.filter(leg -> TripStructureUtils.getRoutingMode(leg).equals(TransportMode.pt))
										.findAny().isPresent())
								.count());
				Assert.assertEquals(2, subtours.size());
			}
		}
	}

	@Test
	public void testBerlinerWithInnerTripsOnly(){

		Person person = SCENARIO.getPopulation().getPersons().get(Id.createPersonId(272337601));
		Assert.assertEquals("berlin", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
		Assert.assertEquals(true, person.getAttributes().getAttribute("livesInProhibitionZone"));

		//test all plans
		//agent has only inner trips
		Assert.assertEquals("person " + person.getId() + " should have 4 plans (with types [2x drt,2x pt])", 4, person.getPlans().size());

		for (Plan plan : person.getPlans()) {
			List<Activity> prActs = getPRActivities(plan);
			Assert.assertEquals(0, prActs.size()); //nr of PR acts
		}
	}



	@Test
	public void testRiderWithDifferentPRStations(){
		// test agent with mode ride that uses different PR stations (as this is not subject to mass conservation)

		Person person = SCENARIO.getPopulation().getPersons().get(Id.createPersonId(116317401));

		//test all plans
		Assert.assertEquals("person " + person.getId() + " should have 5 plans (with types [2x drt,2x pt,ptOnly])", 5, person.getPlans().size());

		for (Plan plan : person.getPlans()) {
			List<Activity> prActs = getPRActivities(plan);
			if(!plan.getType().equals("ptOnly")){
				Assert.assertEquals(2, prActs.size()); //nr of PR acts

				if(plan.getType().contains("closestToInside")) {
					// prStationChoice = closestToInside
					// here it´s actually same PRStation
					Assert.assertEquals(PR_STATIONS.get("Westkreuz/ZOB").getLinkId(),prActs.get(0).getLinkId());
					Assert.assertEquals(PR_STATIONS.get("Westkreuz/ZOB").getCoord(),prActs.get(0).getCoord());
					Assert.assertEquals(PR_STATIONS.get("Westkreuz/ZOB").getLinkId(),prActs.get(1).getLinkId());
					Assert.assertEquals(PR_STATIONS.get("Westkreuz/ZOB").getCoord(),prActs.get(1).getCoord());
				} else {
					// prStationChoice = closestToOutside
					Assert.assertEquals(PR_STATIONS.get("Westkreuz/ZOB").getLinkId(),prActs.get(0).getLinkId());
					Assert.assertEquals(PR_STATIONS.get("Westkreuz/ZOB").getCoord(),prActs.get(0).getCoord());
					Assert.assertEquals(PR_STATIONS.get("Gesundbrunnen").getLinkId(),prActs.get(1).getLinkId());
					Assert.assertEquals(PR_STATIONS.get("Gesundbrunnen").getCoord(),prActs.get(1).getCoord());
				}


			} else {
				Assert.assertEquals(0, prActs.size()); //nr of PR acts
			}
		}

	}


	@Test
	public void testRiderWith2BorderCrossingTripsANDDifferentModes(){
		// this agent is a rider and has two border crossing trips. The one is mode "ride", the other mode "pt" -> should contain 1 PRActivity

		Person person = SCENARIO.getPopulation().getPersons().get(Id.createPersonId(100370701));

		//test all plans
		Assert.assertEquals("person " + person.getId() + " should have 5 plans (with types [2x drt,2x pt,ptOnly])", 5, person.getPlans().size());

		//test all plans
		for (Plan plan : person.getPlans()) {
			List<Activity> prActs = getPRActivities(plan);

			if(!plan.getType().equals("ptOnly")){
				Assert.assertEquals(1, prActs.size()); //nr of PR acts
			} else {
				Assert.assertEquals(0, prActs.size()); //nr of PR acts
			}
		}

	}


	private static List<Activity> getPRActivities(Plan plan) {
		List<Activity> PRacts = new ArrayList<>();
		for(Activity activity: PopulationUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.StagesAsNormalActivities)){
			if(activity.getType().equals(PR_ACTIVITY_TYPE)) PRacts.add(activity);
		}
		return PRacts;
	}

}