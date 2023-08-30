package org.matsim.run.replaceCarByDRT;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.run.replaceCarByDRT.ReplaceCarByDRT.readPRStationFile;

public class PrActivityEventHandlerTest {

    private static Scenario SCENARIO;
    private static PrActivityEventHandler PR_HANDLER;

    private static Logger log = LogManager.getLogger(BerlinNoInnerCarTripsScenarioTest.class);

    private static Set<PRStation> prStations;

    // toggle only for the test. Doesn´t actually change the run settings.
    private static ReplaceCarByDRT.PRStationChoice testCaseToggle;


    @BeforeClass
    public static void main() {
        //TODO: explicitly set parameters - replace testCaseToggle with explicit paramters
        testCaseToggle = ReplaceCarByDRT.PRStationChoice.closestToOutSideActivity;

        String[] configArgs = new String[]{"scenarios/berlin/replaceCarByDRT/noModeChoice/hundekopf-drt-v5.5-1pct.config.xml",
                "--config:controler.lastIteration", "0",
                "--config:plans.inputPlansFile", "replaceCarByDRT.testPlans.xml.gz",
                "--config:controler.outputDirectory", "test/output/org/matsim/run/replaceCarByDRT/closestToOutside"};
        try {
            Config config = RunBerlinNoInnerCarTripsScenario.prepareConfig(configArgs);
            SCENARIO = RunBerlinNoInnerCarTripsScenario.prepareScenario(config);
            Controler controler = RunBerlinNoInnerCarTripsScenario.prepareControler(SCENARIO);
            controler.run();
            PR_HANDLER = controler.getInjector().getInstance(PrActivityEventHandler.class);
            prStations = readPRStationFile(IOUtils.resolveFileOrResource("scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-07-27-pr-stations.tsv"));


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testIsRiderSkipped(){
        //Test 1: isRiderSkipped - Westkreuz/ZOB station is used only by a rider (Agent 116317401). Should be PrActivities == 0

        HashMap<PRStation, MutableInt> activitiesPerPRStation = PR_HANDLER.getActivitiesPerPRStation();
        PRStation searchedStation_test1 = prStations.stream().filter(prStation -> "Westkreuz/ZOB".equals(prStation.getName())).findFirst().orElse(null);

        MutableInt resultMutableInt =  activitiesPerPRStation.entrySet().stream().filter(entry -> entry.getKey().getCoord().equals(searchedStation_test1.getCoord()))
                .map(Map.Entry::getValue).findFirst().orElse(null);

        Assert.assertEquals(0, (resultMutableInt != null) ? resultMutableInt.getValue() : 0);

    }

    @Test
    public void testIsOvernightStayCounted(){
        //Test 2: isOvernightStayCounted - Agent 187328501 leaves car in Jungfernheide PRStation overnight in the morning and in the evening. Testing if this is counted

        Map<PRStation, int[]> carsInPRStationPerMinute = PR_HANDLER.getCarsInPrStationPerMinute();
        PRStation searchedStation_test2 = prStations.stream().filter(prStation -> "Jungfernheide".equals(prStation.getName())).findFirst().orElse(null);

        int[] test2Array = carsInPRStationPerMinute.entrySet().stream().filter(entry -> entry.getKey().getCoord().equals(searchedStation_test2.getCoord()))
                .map(Map.Entry::getValue).findFirst().orElse(null);

        Assert.assertNotEquals(0,test2Array[0]); // firstCarCount
        Assert.assertNotEquals(0,test2Array[test2Array.length - 1]); // lastCarCount

    }

    @Test
    public void testIsArrivingCarSavedANDleavingCarDeleted(){
        //Test 3: isArrivingCarSaved & isLeavingCarDeleted - testing with Brandenburger2PRStationsAgent. PRStation depends on PRStationChoice.
        //Test is dependent on the time the agent enters/leaves the station.

        Map<PRStation, int[]> carsInPRStationPerMinute = PR_HANDLER.getCarsInPrStationPerMinute();

        PRStation searchedStation_test3;
        if(testCaseToggle.equals(ReplaceCarByDRT.PRStationChoice.closestToOutSideActivity)){
            searchedStation_test3 = prStations.stream().filter(prStation -> "Bundesplatz".equals(prStation.getName())).findFirst().orElse(null);
        } else {
            searchedStation_test3 = prStations.stream().filter(prStation -> "".equals(prStation.getName())).findFirst().orElse(null);
        }

        Person person = SCENARIO.getPopulation().getPersons().get(Id.createPersonId(38250801));
        List<ActivityEndEvent> personsPRends = PR_HANDLER.getPrActivityEndEvents().stream()
                .filter(event -> event.getPersonId().equals(person.getId()))
                .collect(Collectors.toList());

        int[] test3Array = carsInPRStationPerMinute.entrySet().stream().filter(entry -> entry.getKey().getCoord().equals(searchedStation_test3.getCoord()))
                .map(Map.Entry::getValue).findFirst().orElse(null);

        // check if car is carsInPRStationPerMinute from the minute when agent has first PRStation ActivityEndEvent
        int enterMinute = (int) Math.floor(personsPRends.get(0).getTime() / 60);
        Assert.assertEquals(0, test3Array[enterMinute - 1]); // carCountAtOneBeforeEnterMinute
        Assert.assertEquals(1,test3Array[enterMinute]); // carCountAtEnterMinute

        // check if car is not in carsInPRStationPerMinute anymore from the minute when agent has second PRStation ActivityEndEvent
        int leaveMinute = ((int) Math.floor(personsPRends.get(1).getTime() / 60)) + 1;
        Assert.assertEquals(1, test3Array[leaveMinute]); // carCountAtLeavingMinute
        Assert.assertEquals(0, test3Array[leaveMinute + 1]); // carCountAtOneAfterLeavingMinute

    }







}
