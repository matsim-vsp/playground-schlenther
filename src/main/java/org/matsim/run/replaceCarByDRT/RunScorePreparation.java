package org.matsim.run.replaceCarByDRT;

import com.opencsv.CSVWriter;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.drt.OpenBerlinIntermodalPtDrtRouterModeIdentifier;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RunScorePreparation {

    private final String INPUT_RUNDIRECTORY;
    private final String INPUT_POPULATION;
    private final String INPUT_INNER_CITY_SHP;
    private final String INPUT_BERLIN_SHP;
    private final URL INPUT_PR_STATIONS;
    private final String INPUT_BOUNDARY_SHP;

    public RunScorePreparation(String runDirectory, String population, String inner_city_shp, String berlin_shp, URL pr_stations, String boundary_shp) {
        this.INPUT_RUNDIRECTORY = runDirectory;
        this.INPUT_POPULATION = population;
        this.INPUT_INNER_CITY_SHP = inner_city_shp;
        this.INPUT_BERLIN_SHP = berlin_shp;
        this.INPUT_PR_STATIONS = pr_stations;
        this.INPUT_BOUNDARY_SHP = boundary_shp;
    }

    public static void main(String[] args) {
        if ( args.length==0 ){
            String runDirectory = "scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/pt,drt/closestToOutside-0.5-1506vehicles-8seats/";
//            String runDirectory = "scenarios/output/baseCaseContinued/"; // Base Case 1pct
            String population = "scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/pt,drt/closestToOutside-0.5-1506vehicles-8seats/closestToOutside-0.5-1506vehicles-8seats.output_plans.xml.gz"; // Car-free Scenario input
//            String population = "scenarios/output/baseCaseContinued/berlin-v5.5-1pct.output_plans.xml.gz"; // Base Case 1pct
            String inner_city_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            String berlin_shp = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
            URL pr_stations = IOUtils.resolveFileOrResource("scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv");
            String boundary_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-boundaries-500m.shp";

            RunScorePreparation scorePreparation = new RunScorePreparation(runDirectory, population, inner_city_shp, berlin_shp, pr_stations, boundary_shp);
            scorePreparation.run();
        } else {
            String runDirectory = args[0];
            String population = args[1];
            String inner_city_shp = args[2];
            String berlin_shp = args[3];
            URL pr_stations = IOUtils.resolveFileOrResource(args[4]);
            String boundary_shp = args[5];

            RunScorePreparation scoresFromPlans = new RunScorePreparation(runDirectory, population, inner_city_shp, berlin_shp, pr_stations, boundary_shp);
            scoresFromPlans.run();
        }



    }

    public void run() {
        Population population = PopulationUtils.readPopulation(INPUT_POPULATION);
        String outputFileName = INPUT_RUNDIRECTORY + "output_plans_selectedPlanScores.tsv";

        List<PreparedGeometry> innerCity = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(INPUT_INNER_CITY_SHP));
        List<PreparedGeometry> berlin = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(INPUT_BERLIN_SHP));
        List<PreparedGeometry> boundaryZone = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(INPUT_BOUNDARY_SHP));

        Set<PRStation> prStations = PRStation.readPRStationFile(INPUT_PR_STATIONS);

        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
            writer.writeNext(new String[]{"person",
                    "executed_score",
                    "home-activity-zone",
                    "income",
                    "mainMode",
                    "travelledDistance",
                    "noOfActivities",
                    "hasPRActivity",
                    "FirstPRStation",
                    "LastPRStation",
                    "livesInsideBoundaryZone",
                    "isCarUser",
                    "home_x",
                    "home_y"});
            for (Person person : population.getPersons().values()) {

                // regional division
                Activity home = getHomeActivity(person.getSelectedPlan());
                String livingLocation = getRegion(home,innerCity,berlin);
                boolean livesInsideBoundaryZone = getIfPersonLivesInsideBoundary(home, boundaryZone);

                // income
                Double income = (Double) PopulationUtils.getPersonAttribute(person,"income");

                // mainMode
                String mainMode = getMainMode(person.getSelectedPlan());
                boolean isCarUser = getIfAgentIsCarUser(person.getSelectedPlan());

                // travelled distance
                Double travelledDistance = getTravelledDistance(person.getSelectedPlan());

                // number of activities (excluding stage activities)
                Double activityCount = getNumberOfActivities(person.getSelectedPlan());

                // has at least one P+R activity?
                boolean prActivity = hasPRActivity(person.getSelectedPlan());

                // get the used PR Station(s)
                String firstPRStation = getFirstPRStation(person.getSelectedPlan(),prStations);
                String lastPRStation = getLastPRStation(person.getSelectedPlan(),prStations);

                // get coordinates of home activity
                String homeX = String.valueOf(home.getCoord().getX());
                String homeY = String.valueOf(home.getCoord().getY());

                writer.writeNext(new String[]{person.getId().toString(),
                        String.valueOf(person.getSelectedPlan().getScore()),
                        livingLocation,
                        String.valueOf(income),
                        mainMode,
                        String.valueOf(travelledDistance),
                        String.valueOf(activityCount),
                        String.valueOf(prActivity),
                        firstPRStation,
                        lastPRStation,
                        String.valueOf(livesInsideBoundaryZone),
                        String.valueOf(isCarUser),
                        homeX,
                        homeY}
                );
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getRegion(Activity home, List<PreparedGeometry> innerCity, List<PreparedGeometry> berlin) {

        if(ShpGeometryUtils.isCoordInPreparedGeometries(home.getCoord(), innerCity)){
            return "innerCity";
        }

        if(ShpGeometryUtils.isCoordInPreparedGeometries(home.getCoord(), berlin)){
            if(!ShpGeometryUtils.isCoordInPreparedGeometries(home.getCoord(), innerCity)){
                return "BerlinButNotInnerCity";
            }
            else {
                throw new IllegalStateException("inner city should be catched above");
            }
        } else {
            return "Brandenburg";
        }
    }

    private static boolean getIfPersonLivesInsideBoundary(Activity home, List<PreparedGeometry> innerCityBoundary) {
        if(ShpGeometryUtils.isCoordInPreparedGeometries(home.getCoord(), innerCityBoundary)) {
            return true;
        } else {
            return false;
        }
    }


    private static String getFirstPRStation(Plan plan, Set<PRStation> prStations){
        List<PlanElement> planElements = plan.getPlanElements();
        for (PlanElement planElement : planElements){
            if (planElement instanceof Activity){
                String activity = ((Activity) planElement).getType();
                if (activity.equals("P+R")){
                    Coord coord = ((Activity) planElement).getCoord();

                    for (PRStation prStation : prStations){
                        if(coord.equals(prStation.getCoord())){
                            return prStation.getName();
                        }
                    }

                }
            }
        }
        return "";
    }


    private static String getLastPRStation(Plan plan, Set<PRStation> prStations){
        List<PlanElement> planElements = plan.getPlanElements();
        String lastPRStation = "";
        for (PlanElement planElement : planElements){
            if (planElement instanceof Activity){
                String activity = ((Activity) planElement).getType();
                if (activity.equals("P+R")){
                    Coord coord = ((Activity) planElement).getCoord();
                    for (PRStation prStation : prStations){
                        if(coord.equals(prStation.getCoord())){
                            lastPRStation = prStation.getName();
                        }
                    }

                }
            }
        }
        return lastPRStation;
    }

    private static boolean hasPRActivity(Plan plan){
        List<PlanElement> planElements = plan.getPlanElements();
        for (int i=0;i<planElements.size();i++){
            if (planElements.get(i) instanceof Activity){
                String activity = ((Activity) planElements.get(i)).getType();
                if (activity.equals("P+R")){
                    return true;
                }
            }
        }
        return false;
    }

    private static Double getTravelledDistance(Plan plan) {
        List<Leg> legs = PopulationUtils.getLegs(plan);
        Double distance = 0.0;

        for (Leg leg : legs) {
            distance += leg.getRoute().getDistance();
        }

        return distance;
    }

    private static Double getNumberOfActivities(Plan plan) {
        List<Activity> activities = PopulationUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
        Double activityCount = 0.0;

        for (Activity activity : activities) {
            activityCount ++;
        }

        return activityCount;
    }

    private static String getMainMode(Plan plan) {
        OpenBerlinIntermodalPtDrtRouterModeIdentifier mainModeIdentifier = new OpenBerlinIntermodalPtDrtRouterModeIdentifier();
        List<? extends PlanElement> planElements = plan.getPlanElements();

        if (TripStructureUtils.getTrips(plan).isEmpty()) {
            return "";
        }

        String mainMode = mainModeIdentifier.identifyMainMode(planElements);
        return mainMode;
    }

    private static Activity getHomeActivity(Plan selectedPlan) {
        List<Activity> acts = TripStructureUtils.getActivities(selectedPlan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

        return acts.stream()
                .filter(act -> act.getType().contains("home"))
                .findFirst().orElse(acts.get(0));
    }

    private static boolean getIfAgentIsCarUser(Plan plan) {
        List<Leg> legs = PopulationUtils.getLegs(plan);
        if (legs.size() == 0){
            return false;
        }

        List<String> modes = new ArrayList<>();

        for (Leg leg : legs) {
            if(leg.getMode().contains("car")) {
                return true;
            }
        }

        return false;
    }

    // can be used instead of mainMode
    private static String getLongestDistanceMode(Plan plan) {
        List<Leg> legs = PopulationUtils.getLegs(plan);
        if (legs.size() == 0){
            return "";
        }

        List<String> modes = new ArrayList<>();
        double currentLongestShareDistance = Double.MIN_VALUE;
        String currentModeWithLongestShare = "";

        for (Leg leg : legs) {
            modes.add(leg.getMode());
            final double legDist = leg.getRoute().getDistance();

            if (legDist > currentLongestShareDistance) {
                currentLongestShareDistance = legDist;
                currentModeWithLongestShare = leg.getMode();
            }
        }

        return currentModeWithLongestShare;
    }
}


