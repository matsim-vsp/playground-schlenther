package org.matsim.run.replaceCarByDRT;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.matsim.run.replaceCarByDRT.ReplaceCarByDRT.PR_ACTIVITY_TYPE;

public class BerlinNoInnerCarTripsScenarioTest {

	private static Scenario scenario;
	private static PrActivityEventHandler prActivityEventHandler;

	private static Logger log = LogManager.getLogger(BerlinNoInnerCarTripsScenarioTest.class);

	@BeforeClass
	public static void main() {
		String[] configArgs = new String[]{"scenarios/berlin/replaceCarByDRT/noModeChoice/hundekopf-drt-v5.5-0.1pct.config.test.xml",
				"--config:controler.lastIteration", "0",
//				"--config:plans.inputPlansFile", "replaceCarByDRT.testPlans.xml",
				"--config:controler.outputDirectory", "test/output/org/matsim/run/replaceCarByDRT/"};
		try {
			Config config = RunBerlinNoInnerCarTripsScenario.prepareConfig(configArgs);
			scenario = RunBerlinNoInnerCarTripsScenario.prepareScenario(config);
			Controler controler = RunBerlinNoInnerCarTripsScenario.prepareControler(scenario);
			controler.run();
			prActivityEventHandler = controler.getInjector().getInstance(PrActivityEventHandler.class);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testNumberOfPRActivities(){
		Assert.assertEquals(42, prActivityEventHandler.getPrActivityEndEvents().size());
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

	public void testBerlinerWithExternalTripsOnly(){

		Person person = scenario.getPopulation().getPersons().get(Id.createPersonId("340500501"));

		Assert.assertEquals("berlin", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));

		for (Plan plan : person.getPlans()) {
			int nrOfPlannedPRActivitites = PopulationUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.StagesAsNormalActivities).stream()
					.filter(activity -> activity.getType().equals(PR_ACTIVITY_TYPE))
					.collect(Collectors.toSet()).size();
			Assert.assertEquals("person " + person.getId() + " should not plan PR activities, but does so in at least one of its plans!", 0, nrOfPlannedPRActivitites);
			Assert.assertEquals("person " + person.getId() + " should not be affected by prohibition zone", "not-affected", plan.getType());
		}

		Assert.assertFalse("there should be no ActivityEndEvent for activityType = " + PR_ACTIVITY_TYPE + " with personId= " + person.getId(),
				prActivityEventHandler.getPrActivityEndEvents().stream()
				.filter(event -> event.getPersonId().equals(person.getId()))
				.findAny().isPresent());
	}

	public void testBrandenburgerWith2PRActivities(){

		Person person = scenario.getPopulation().getPersons().get(Id.createPersonId("38250801"));

		Assert.assertEquals("brandenburg", PopulationUtils.getPersonAttribute(person, "home-activity-zone"));

		for (Plan plan : person.getPlans()) {
			int nrOfPlannedPRActivitites = PopulationUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.StagesAsNormalActivities).stream()
					.filter(activity -> activity.getType().equals(PR_ACTIVITY_TYPE))
					.collect(Collectors.toSet()).size();
			Assert.assertEquals("person " + person.getId() + " should not plan PR activities, but does so in at least one of its plans!", 2, nrOfPlannedPRActivitites);
		}

		Assert.assertEquals("there should be 2 ActivityEndEvents for activityType = " + PR_ACTIVITY_TYPE + " with personId= " + person.getId(),
				2,
				prActivityEventHandler.getPrActivityEndEvents().stream()
						.filter(event -> event.getPersonId().equals(person.getId()))
						.count());

		Assert.assertEquals(Id.createLinkId(6727),prActivityEventHandler.getPrActivityEndEvents().get(0).getLinkId());
		Assert.assertEquals(Id.createLinkId(6727),prActivityEventHandler.getPrActivityEndEvents().get(1).getLinkId());
	}

	//TODO: test agent with mode ride that uses different PR stations (as this is not subject to mass conservation)

	//TODO: test agent that uses pt only plan (?)

	//TODO write a copy of this test class, set up simulation with PRStationChoice.closesToInside, check for PR activity locations


}