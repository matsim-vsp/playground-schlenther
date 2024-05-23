package org.matsim.run.replaceCarByDRT;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.analysis.PrActivityEventHandler;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.matsim.run.replaceCarByDRT.PRStation.readPRStationFile;

public class PrActivityEventHandlerTest {

    private static Logger log = LogManager.getLogger(ReplaceCarByDRTTest.class);

    @Test
    public void testPrActivityEventHandler() {

        Set<PRStation> prStations = readPRStationFile(IOUtils.resolveFileOrResource("scenarios/berlin-v6.1/berlin-v6.1-pr-stations-ring.tsv"));

        Iterator<PRStation> iterator = prStations.iterator();
        PRStation prStation1 = iterator.next();
        PRStation prStation2 = iterator.next();
        PRStation prStation3 = iterator.next();

        PrActivityEventHandler handler = new PrActivityEventHandler(IOUtils.resolveFileOrResource("scenarios/berlin-v6.1/berlin-v6.1-pr-stations-ring.tsv"));
        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(handler);
        events.initProcessing();

        //Test 1: isRiderSkipped - PR station is used only by a rider. Should be PrActivities == 0

        Id<Link> linkId1 = Id.create(11, Link.class);
        Id<Person> persId1 = Id.create(1,Person.class);
        Id<ActivityFacility> facId1 = Id.create(21, ActivityFacility.class);

        handler.handleEvent(new PersonDepartureEvent(1.0*60,persId1,linkId1,"ride","ride"));
        handler.handleEvent(new ActivityEndEvent(1.1*60,persId1,linkId1,facId1,"P+R", prStation1.getCoord()));
        handler.handleEvent(new PersonDepartureEvent(1.2*60,persId1,linkId1,"walk","walk"));
        handler.handleEvent(new ActivityEndEvent(1.3*60,persId1,linkId1,facId1,"other",new Coord(50,40)));
        handler.handleEvent(new PersonDepartureEvent(1.4*60,persId1,linkId1,"walk","walk"));
        handler.handleEvent(new ActivityEndEvent(1.5*60,persId1,linkId1,facId1,"P+R", prStation1.getCoord()));
        handler.handleEvent(new PersonDepartureEvent(1.6*60,persId1,linkId1,"ride","ride"));

        MutableInt resultTest1 =  handler.getActivitiesPerPRStation().entrySet().stream().filter(entry -> entry.getKey().getCoord().equals(prStation1.getCoord()))
            .map(Map.Entry::getValue).findFirst().orElse(null);
        Assert.assertEquals(0, (resultTest1 != null) ? resultTest1.getValue() : 0);


        //Test 2: isOvernightStayCounted - Agent leaves car in PRStation overnight in the morning and in the evening. Testing if this is counted

        Id<Link> linkId2 = Id.create(12, Link.class);
        Id<Person> persId2 = Id.create(2,Person.class);
        Id<ActivityFacility> facId2 = Id.create(22, ActivityFacility.class);

        handler.handleEvent(new PersonDepartureEvent(2.0*3600,persId2,linkId2,"walk","walk"));
        handler.handleEvent(new ActivityEndEvent(2.1*3600,persId2,linkId2,facId2,"P+R", prStation2.getCoord()));
        handler.handleEvent(new PersonDepartureEvent(2.2*3600,persId2,linkId2,"car","car"));
        handler.handleEvent(new ActivityEndEvent(2.3*3600,persId2,linkId2,facId2,"other",new Coord(50,40)));
        handler.handleEvent(new PersonDepartureEvent(2.4*3600,persId2,linkId2,"car","car"));
        handler.handleEvent(new ActivityEndEvent(2.5*3600,persId2,linkId2,facId2,"P+R", prStation2.getCoord()));
        handler.handleEvent(new PersonDepartureEvent(2.6*3600,persId2,linkId2,"walk","walk"));

        int[] test2Array = handler.getCarsInPrStationPerMinute().entrySet().stream().filter(entry -> entry.getKey().getCoord().equals(prStation2.getCoord()))
                .map(Map.Entry::getValue).findFirst().orElse(null);

        Assert.assertEquals(1,test2Array[0]); // firstCarCount
        Assert.assertEquals(0,test2Array[(int) Math.floor(2.3*60)]);
        Assert.assertEquals(1,test2Array[test2Array.length - 1]); // lastCarCount

        //Test 3: isArrivingCarSaved & isLeavingCarDeleted - Test is dependent on the time the agent enters/leaves the station.

        Id<Link> linkId3 = Id.create(13, Link.class);
        Id<Person> persId3 = Id.create(3,Person.class);
        Id<ActivityFacility> facId3 = Id.create(23, ActivityFacility.class);

        ActivityEndEvent enterEvent = new ActivityEndEvent(3.1*3600,persId3,linkId3,facId3,"P+R", prStation3.getCoord());
        ActivityEndEvent leaveEvent = new ActivityEndEvent(3.5*3600,persId3,linkId3,facId3,"P+R", prStation3.getCoord());

        handler.handleEvent(new PersonDepartureEvent(3.0*3600,persId3,linkId3,"car","car"));
        handler.handleEvent(enterEvent);
        handler.handleEvent(new PersonDepartureEvent(3.2*3600,persId3,linkId3,"walk","walk"));
        handler.handleEvent(new ActivityEndEvent(3.3*3600,persId3,linkId3,facId3,"other",new Coord(50,40)));
        handler.handleEvent(new PersonDepartureEvent(3.4*3600,persId3,linkId3,"walk","walk"));
        handler.handleEvent(leaveEvent);
        handler.handleEvent(new PersonDepartureEvent(3.6*3600,persId3,linkId3,"car","car"));

        int[] test3Array = handler.getCarsInPrStationPerMinute().entrySet().stream().filter(entry -> entry.getKey().getCoord().equals(prStation3.getCoord()))
                .map(Map.Entry::getValue).findFirst().orElse(null);

        // check if car is carsInPRStationPerMinute from the minute when agent has first PRStation ActivityEndEvent
        int enterMinute = (int) Math.floor(enterEvent.getTime() / 60);
        Assert.assertEquals(0, test3Array[enterMinute - 1]); // carCountAtOneBeforeEnterMinute
        Assert.assertEquals(1,test3Array[enterMinute]); // carCountAtEnterMinute

        // check if car is not in carsInPRStationPerMinute anymore from the minute when agent has second PRStation ActivityEndEvent
        int leaveMinute = ((int) Math.floor(leaveEvent.getTime() / 60)) - 1;
        Assert.assertEquals(1, test3Array[leaveMinute]); // carCountAtLeavingMinute
        Assert.assertEquals(0, test3Array[leaveMinute + 1]); // carCountAtOneAfterLeavingMinute
    }
}
