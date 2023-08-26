package org.matsim.run.replaceCarByDRT;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.matsim.run.replaceCarByDRT.ReplaceCarByDRT.PR_ACTIVITY_TYPE;


//TODO write a copy of this test class, set up simulation with PRStationChoice.closesToInside, check for PR activity locations
public class BerlinNoInnerCarTripsScenarioTest {

	private static Scenario scenario;
	private static PrActivityEventHandler prActivityEventHandler;

	private static Logger log = LogManager.getLogger(BerlinNoInnerCarTripsScenarioTest.class);

	@BeforeClass
	public static void main() {
		//TODO: explicitly set parameters
		String[] configArgs = new String[]{"scenarios/berlin/replaceCarByDRT/noModeChoice/hundekopf-drt-v5.5-0.1pct.config.test.xml",
				"--config:controler.lastIteration", "0",
				"--config:plans.inputPlansFile", "replaceCarByDRT.testPlans.xml.gz",
				"--config:controler.outputDirectory", "test/output/org/matsim/run/replaceCarByDRT/closestToOutside"};
		try {
			Config config = RunBerlinNoInnerCarTripsScenario.prepareConfig(configArgs);
//			DrtConfigGroup.getSingleModeDrtConfig(config).setNumberOfThreads(4);
			scenario = RunBerlinNoInnerCarTripsScenario.prepareScenario(config);
			Controler controler = RunBerlinNoInnerCarTripsScenario.prepareControler(scenario);
			controler.run();
			prActivityEventHandler = controler.getInjector().getInstance(PrActivityEventHandler.class);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testTotalNumberOfPRActivities(){
		Assert.assertEquals(6, prActivityEventHandler.getPrActivityEndEvents().size());
	}

	@Test
	public void testAgentAttributesAndPlanTypes(){
		Set<String> violations = new HashSet<>();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if(!PopulationUtils.getSubpopulation(person).equals("person")) continue;

			if (person.getAttributes().getAttribute("livesInProhibitionZone") == null){
				violations.add("person " + person.getId() + "does not have livesInProhibitionZone attribute");
			}

			Set<String> planTypes = person.getPlans().stream()
					.map(plan -> plan.getType())
					.collect(Collectors.toSet());
			//either only 1 plan type (then it is "not-affected") or n + 1 plan types where n = nr of replacing modes
			//in our standard case, replacing modes are pt and drt, so this means we either should have 3 plan types or 1
			if (planTypes.size() != 3 && !(planTypes.size() == 1 && planTypes.contains("not-affected"))){
				violations.add("person " + person.getId() + " has unexpected number of plan types. plantypes = " + planTypes);
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

		Person person = scenario.getPopulation().getPersons().get(Id.createPersonId(340500501));

		Assert.assertEquals("berlin", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));

		//test all plans
		for (Plan plan : person.getPlans()) {
			assertPRActivitiesCountToX(plan, 0);
			Assert.assertEquals("person " + person.getId() + " should not be affected by prohibition zone", "not-affected", plan.getType());
		}

		//test events
		assertPRActivityEndEventsCountToX(person, 0);
	}

	@Test
	public void testBrandenburgerWith2PRActivities(){

		Person person = scenario.getPopulation().getPersons().get(Id.createPersonId(38250801));

		Assert.assertEquals("brandenburg", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
		Assert.assertEquals(false, person.getAttributes().getAttribute("livesInProhibitionZone"));

		//test all plans
		for (Plan plan : person.getPlans()) {
			if(!plan.getType().equals("ptOnly")){
				assertPRActivitiesCountToX(plan, 2);
			} else {
				assertPRActivitiesCountToX(plan, 0);
			}
		}

		//test events
		assertPRActivityEndEventsCountToX(person, 2);
		List<ActivityEndEvent> personsPRends = prActivityEventHandler.getPrActivityEndEvents().stream()
				.filter(event -> event.getPersonId().equals(person.getId()))
				.collect(Collectors.toList());
		Assert.assertEquals(Id.createLinkId(6726),personsPRends.get(0).getLinkId());
		Assert.assertEquals(Id.createLinkId(6726),personsPRends.get(1).getLinkId());
	}

	@Test
	public void testInhabitantOfProhibitionZoneWithMultipleBorderCrossings(){
		//TODO: test agent that has multiple trips across the border of prohibition zone (at best 4 or more - possibly at different PR stations). test that firstPRStation = lasPRStation


	}

	@Test
	public void testInhabitantOfProhibitionZoneWithOutSideCarSubtour(){
		//this agent lives in the prohibition zone, travels to work outside,
		// than has a subtour outside that could normally be a car subtour, but unfortunately the coordinates of both work differ slightly
		// meaning that the ptOnly plan replaces ALL trips by pt trips, inclduing the subtour that could remain car!
		Person person = scenario.getPopulation().getPersons().get(Id.createPersonId(259563501));
		Assert.assertEquals("expecting agent's home-activity-zone to be berlin", "berlin", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
		Assert.assertEquals(true, person.getAttributes().getAttribute("livesInProhibitionZone"));

		for (Plan plan : person.getPlans()) {
			if(!plan.getType().equals("ptOnly")){
				assertPRActivitiesCountToX(plan, 2);
			} else {
				assertPRActivitiesCountToX(plan, 0);
			}
		}

	}

	@Test
	public void testInhabitantPfProhibitionZoneWithOutsideANDCrossingSubtours(){
		//this agent lives in berlin outside the prohibiton zone
		//.. has one outside subtour and one border-crossing subtour, both start+end @ home
		Person person = scenario.getPopulation().getPersons().get(Id.createPersonId(446367901));

		Assert.assertEquals("berlin", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
		Assert.assertEquals(false, person.getAttributes().getAttribute("livesInProhibitionZone"));

		//test all plans
		for (Plan plan : person.getPlans()) {
			if(!plan.getType().equals("ptOnly")){
				assertPRActivitiesCountToX(plan, 2);
			} else {
				assertPRActivitiesCountToX(plan, 0);
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

		//test events
		assertPRActivityEndEventsCountToX(person, 2);
	}

	@Test
	public void testBerlinerWithInnerTripsOnly(){

		Person person = scenario.getPopulation().getPersons().get(Id.createPersonId(272337601));

		Assert.assertEquals("berlin", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));
		Assert.assertEquals(true, person.getAttributes().getAttribute("livesInProhibitionZone"));

		//test all plans
		//agent has only inner trips
		for (Plan plan : person.getPlans()) {
			assertPRActivitiesCountToX(plan, 0);
		}

		//test events
		assertPRActivityEndEventsCountToX(person, 0);
	}


	//TODO test agent (lives ooutside prohibition zone) with multiple (best 4 or more) border-crossing trips. First and last PR station can be different!

	//TODO: test agent with mode ride that uses different PR stations (as this is not subject to mass conservation)

	//TODO: test agent that uses ride in combination with other modes in one subtour that touches prohibition zone

	//TODO: test agent that uses pt only plan (?)

	private static void assertPRActivityEndEventsCountToX(Person person, int expected) {
		Assert.assertEquals("there should be exactly " + expected + " ActivityEndEvents for activityType = " + PR_ACTIVITY_TYPE + " with personId= " + person.getId(),
				expected,
				prActivityEventHandler.getPrActivityEndEvents().stream()
						.filter(event -> event.getPersonId().equals(person.getId()))
						.count());
	}

	private static void assertPRActivitiesCountToX(Plan plan, int expected) {
		int nrOfPlannedPRActivitites = PopulationUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.StagesAsNormalActivities).stream()
				.filter(activity -> activity.getType().equals(PR_ACTIVITY_TYPE))
				.collect(Collectors.toSet()).size();
		Assert.assertEquals("person " + plan.getPerson().getId() + " should plan exactly " + expected + " PR activities", expected, nrOfPlannedPRActivitites);
	}

}