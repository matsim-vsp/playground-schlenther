package org.matsim.analysis;

import com.opencsv.CSVWriter;
import com.opencsv.bean.util.OpencsvUtils;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ScoresFromPlans2CSV {

   // private static final String INPUT_POPULATION = "scenarios/output/inside-allow-0.5-1506vehicles-8seats/inside-allow-0.5-1506vehicles-8seats.output_plans.xml.gz"; // Car-free Scenario input
    private static final String INPUT_POPULATION = "scenarios/output/baseCase/berlin-v5.5.3-1pct.output_plans.xml.gz"; // Base Case Input
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
                    "livesInBerlin",
                    "livesInInnerCity",
                    "livesInBerlinButNotInnerCity",
                    "livesInBrandenburg"});
            for (Person person : population.getPersons().values()) {

                Activity home = getHomeActivity(person.getSelectedPlan());

                boolean livesInInnerCity = false;
                boolean livesInBerlin = false;
                boolean livesInBerlinButNotInnerCity = false;
                boolean livesInBrandenburg = false;

                if(ShpGeometryUtils.isCoordInPreparedGeometries(home.getCoord(), innerCity)){
                    livesInInnerCity = true;
                }
                if(ShpGeometryUtils.isCoordInPreparedGeometries(home.getCoord(), berlin)){
                    livesInBerlin = true;
                }
                if(ShpGeometryUtils.isCoordInPreparedGeometries(home.getCoord(), berlin)){
                    if(!ShpGeometryUtils.isCoordInPreparedGeometries(home.getCoord(), innerCity)){
                        livesInBerlinButNotInnerCity = true;
                    }
                }
                if(!ShpGeometryUtils.isCoordInPreparedGeometries(home.getCoord(), berlin)){
                    if(!ShpGeometryUtils.isCoordInPreparedGeometries(home.getCoord(), innerCity)){
                        livesInBrandenburg = true;
                    }
                }


                writer.writeNext(new String[]{person.getId().toString(),
                        String.valueOf(person.getSelectedPlan().getScore()),
                        String.valueOf(livesInBerlin),
                        String.valueOf(livesInInnerCity),
                        String.valueOf(livesInBerlinButNotInnerCity),
                        String.valueOf(livesInBrandenburg)}
                );
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Activity getHomeActivity(Plan selectedPlan) {
        List<Activity> acts = TripStructureUtils.getActivities(selectedPlan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

        return acts.stream()
                .filter(act -> act.getType().contains("home"))
                .findFirst().orElse(acts.get(0));
    }
}
