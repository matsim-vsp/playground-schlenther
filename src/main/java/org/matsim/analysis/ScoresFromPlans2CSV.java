package org.matsim.analysis;

import com.opencsv.CSVWriter;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ScoresFromPlans2CSV {

    private static final String INPUT_POPULATION = "scenarios/output/berlin-v5.5-sample/inside-allow-0.5-1506vehicles-8seats.output_plans.xml.gz"; // Sample input
    // private static final String INPUT_POPULATION = "scenarios/output/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_plans.xml.gz"; // Car-free Scenario input
    // private static final String INPUT_POPULATION = "scenarios/output/baseCase/berlin-v5.5.3-1pct.output_plans.xml.gz"; // Base Case Input
    private static final String INPUT_INNER_CITY_SHP = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
    private static final String INPUT_BERLIN_SHP = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";

    public static void main(String[] args) {

        Population population = PopulationUtils.readPopulation(INPUT_POPULATION);
        String outputFileName = INPUT_POPULATION.substring(0, INPUT_POPULATION.lastIndexOf(".xml")) + "_selectedPlanScores.tsv";

        List<PreparedGeometry> innerCity = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(INPUT_INNER_CITY_SHP));
        List<PreparedGeometry> berlin = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(INPUT_BERLIN_SHP));

        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
            writer.writeNext(new String[]{"agentId",
                    "selectedPlanScore",
                    "livingLocation",
                    "income",
                    "mainMode",
                    "travelledDistance",
                    "noOfActivities",
                    "hasPRActivity"});
            for (Person person : population.getPersons().values()) {

                // regional division
                Activity home = getHomeActivity(person.getSelectedPlan());
                String livingLocation = getRegion(home,innerCity,berlin);

                // income
                Double income = (Double) PopulationUtils.getPersonAttribute(person,"income"); //Do not cast, how else can I do it? -- TS: you have to cast here..

                // main mode
                String mainMode = getMainMode(person.getSelectedPlan());

                // travelled distance
                Double travelledDistance = getTravelledDistance(person.getSelectedPlan());

                // number of activities (excluding stage activities)
                Double activityCount = getNumberOfActivities(person.getSelectedPlan());

                // at least one P+R activity?
                boolean prActivity = hasPRActivity(person.getSelectedPlan());


                writer.writeNext(new String[]{person.getId().toString(),
                        String.valueOf(person.getSelectedPlan().getScore()),
                        livingLocation,
                        String.valueOf(income),
                        mainMode,
                        String.valueOf(travelledDistance),
                        String.valueOf(activityCount),
                        String.valueOf(prActivity)}
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
        List<Leg> legs = PopulationUtils.getLegs(plan);
        if (legs.size() == 0){
            return "";
        }

        return TripStructureUtils.identifyMainMode(PopulationUtils.getLegs(plan));
    }

    private static Activity getHomeActivity(Plan selectedPlan) {
        List<Activity> acts = TripStructureUtils.getActivities(selectedPlan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

        return acts.stream()
                .filter(act -> act.getType().contains("home"))
                .findFirst().orElse(acts.get(0));
    }
}
