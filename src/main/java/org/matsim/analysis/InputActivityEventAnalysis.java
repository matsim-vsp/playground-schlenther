package org.matsim.analysis;

import com.opencsv.CSVWriter;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InputActivityEventAnalysis {

    // private static final String INPUT_POPULATION = "scenarios/berlin/replaceCarByDRT/noModeChoice/berlin-v5.5-sample.plans.xml.gz"; // Sample input
    private static final String INPUT_POPULATION = "scenarios/berlin/baseCase/berlin-v5.5-1pct.plans.xml.gz"; // Base case input

    public static void main(String[] args) {
        Population population = PopulationUtils.readPopulation(INPUT_POPULATION);
        String outputFileName = INPUT_POPULATION.substring(0, INPUT_POPULATION.lastIndexOf(".xml")) + "_activityEventTypes.tsv";

        HashMap<String, Integer> activityTypesEndTimes = new HashMap<>();
        HashMap<String, Integer> activityTypesMaxDurations = new HashMap<>();

        for (Person person : population.getPersons().values()) {

            List<Activity> activities = PopulationUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

            for (Activity activity : activities) {

                String activityType = "";

                if (activity.getType().contains("_")) {
                    activityType = activity.getType().substring(0, activity.getType().indexOf("_"));
                }
                if (!activity.getType().contains("_")) {
                    activityType = activity.getType();
                }

                if (!activityTypesEndTimes.containsKey(activityType)){
                    activityTypesEndTimes.put(activityType,0);
                    activityTypesMaxDurations.put(activityType,0);
                }

                for (String actType : activityTypesEndTimes.keySet()) {

                    if (activityType.equals(actType)) {
                        if (activity.getEndTime().isDefined()){
                            activityTypesEndTimes.computeIfPresent(actType, (k, v) -> v + 1);
                        }

                        if (activity.getMaximumDuration().isDefined()){
                            activityTypesMaxDurations.computeIfPresent(actType, (k, v) -> v + 1);
                        }
                    }
                }
            }
        }

        // total amount
        Integer totalActEndTimes = 0;
        Integer totalActMaxDur = 0;
        for (String actType : activityTypesEndTimes.keySet()) {
            totalActEndTimes += activityTypesEndTimes.get(actType);
            totalActMaxDur += activityTypesMaxDurations.get(actType);
        }

        // Write CSV file
        try {
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFileName)), '\t', CSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
            writer.writeNext(new String[]{"activityType","activityEndTimes",
                    "activityMaxDurations"});

            for (String actType : activityTypesEndTimes.keySet()) {
                writer.writeNext(new String[] {
                        actType,
                        String.valueOf(activityTypesEndTimes.get(actType)),
                        String.valueOf(activityTypesMaxDurations.get(actType))
                });
            }

            writer.writeNext(new String[]{
                    "total",
                    String.valueOf(totalActEndTimes),
                    String.valueOf(totalActMaxDur)
            });

            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
