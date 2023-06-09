package org.matsim.run.replaceCarByDRT;

import com.opencsv.CSVWriter;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.analysis.RunOfflineNoiseAnalysis;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.drt.OpenBerlinIntermodalPtDrtRouterModeIdentifier;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.core.router.MainModeIdentifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ScoresFromPlans2CSV {

    private final String INPUT_RUNDIRECTORY;
    private final String INPUT_POPULATION;
    private final String INPUT_INNER_CITY_SHP;
    private final String INPUT_BERLIN_SHP;
    private final String INPUT_PR_STATIONS;

    public ScoresFromPlans2CSV(String runDirectory, String population, String inner_city_shp, String berlin_shp, String pr_stations) {
        this.INPUT_RUNDIRECTORY = runDirectory;
        this.INPUT_POPULATION = population;
        this.INPUT_INNER_CITY_SHP = inner_city_shp;
        this.INPUT_BERLIN_SHP = berlin_shp;
        this.INPUT_PR_STATIONS = pr_stations;
    }

    public static void main(String[] args) {
        String runDirectory = "scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/pt,drt/closestToOutside-0.5-1506vehicles-8seats/";
        // private static final String INPUT_POPULATION = "scenarios/output/old-runs/berlin-v5.5-sample/inside-allow-0.5-1506vehicles-8seats.output_plans.xml.gz"; // Sample input
        String population = "scenarios/output/closestToOutSideActivity/shareVehAtStations-0.5/pt,drt/closestToOutside-0.5-1506vehicles-8seats/closestToOutside-0.5-1506vehicles-8seats.output_plans.xml.gz"; // Car-free Scenario input
        // private static final String INPUT_POPULATION = "scenarios/output/baseCaseContinued/berlin-v5.5-1pct.output_plans.xml.gz"; // Base Case Input
        String inner_city_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
        String berlin_shp = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
        String pr_stations = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";

        ScoresFromPlans2CSV scoresFromPlans = new ScoresFromPlans2CSV(runDirectory, population, inner_city_shp, berlin_shp, pr_stations);
        scoresFromPlans.run();
    }

    void run() {
        Population population = PopulationUtils.readPopulation(INPUT_POPULATION);
        String outputFileName = INPUT_RUNDIRECTORY + "output_plans_selectedPlanScores.tsv";

        List<PreparedGeometry> innerCity = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(INPUT_INNER_CITY_SHP));
        List<PreparedGeometry> berlin = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(INPUT_BERLIN_SHP));

        Set<PRStation> prStations = ReplaceCarByDRT.readPRStationFile(IOUtils.resolveFileOrResource(INPUT_PR_STATIONS));

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
                    "LastPRStation"});
            for (Person person : population.getPersons().values()) {

                // regional division
                Activity home = getHomeActivity(person.getSelectedPlan());
                String livingLocation = getRegion(home,innerCity,berlin);

                // income
                Double income = (Double) PopulationUtils.getPersonAttribute(person,"income");

                // mainMode
                String mainMode = getMainMode(person.getSelectedPlan());

                // travelled distance
                Double travelledDistance = getTravelledDistance(person.getSelectedPlan());

                // number of activities (excluding stage activities)
                Double activityCount = getNumberOfActivities(person.getSelectedPlan());

                // has at least one P+R activity?
                boolean prActivity = hasPRActivity(person.getSelectedPlan());

                // get the used PR Station(s)
                String firstPRStation = getFirstPRStation(person.getSelectedPlan(),prStations);
                String lastPRStation = getLastPRStation(person.getSelectedPlan(),prStations);


                writer.writeNext(new String[]{person.getId().toString(),
                        String.valueOf(person.getSelectedPlan().getScore()),
                        livingLocation,
                        String.valueOf(income),
                        mainMode,
                        String.valueOf(travelledDistance),
                        String.valueOf(activityCount),
                        String.valueOf(prActivity),
                        firstPRStation,
                        lastPRStation}
                );
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getRegion(Activity home, List<PreparedGeometry> innerCity, List<PreparedGeometry> berlin) {

        String livingLocation = new String();

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


