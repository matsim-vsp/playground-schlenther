package org.matsim.analysis;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RunTripsPreparation {

    private final String runDirectory;
    private final String runId;
    private final String population;
    private final String inner_city_shp;
    private final String berlin_shp;
    private final String pr_stations;
    private final String rScriptCommand;

    private static final Logger log = Logger.getLogger(RunTripsPreparation.class);

    public RunTripsPreparation(String runDirectory, String runId, String population, String inner_city_shp, String berlin_shp, String pr_stations, String rScriptCommand) {
        this.runDirectory = runDirectory;
        this.runId = runId;
        this.population = population;
        this.inner_city_shp = inner_city_shp;
        this.berlin_shp = berlin_shp;
        this.pr_stations = pr_stations;
        this.rScriptCommand = rScriptCommand;
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        if ( args.length==0 ){
            //String runDirectory = "scenarios/output/sample/";
            //String runId = "sample-run";
            String rScriptCommand = "C:/Program Files/R/R-4.2.2/bin/Rscript.exe";
            String inner_city_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            String berlin_shp = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
            String pr_stations = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";

            List<String> sensitivityRuns = new ArrayList<String>();
//            sensitivityRuns.add("vehStationShare-100");
//            sensitivityRuns.add("vehStationShare-50");
//            sensitivityRuns.add("stationChoice-closestToOutside");
//            sensitivityRuns.add("stationChoice-both");
//            sensitivityRuns.add("replacingModes-pt-bicycle");
//            sensitivityRuns.add("replacingModes-all");
//            sensitivityRuns.add("optimum-true");
//            sensitivityRuns.add("kPrStations-3");
//            sensitivityRuns.add("extraPrStationPlan-true");
//            sensitivityRuns.add("enforceMassConservation-false");
//            sensitivityRuns.add("stationChoice-closestToInside2");
            sensitivityRuns.add("extraPtPlan-true");

//            sensitivityRuns.add("roadtypesAllowed-all");
//            sensitivityRuns.add("roadtypesAllowed-motorway");


            for(String run : sensitivityRuns) {
                String runId = run;
                String runDirectory = "scenarios/output/runs-2023-09-01/1pct/" + run + "/";
                String population = runDirectory + runId + ".output_plans.xml.gz";

                RunTripsPreparation postprocessor = new RunTripsPreparation(runDirectory, runId, population, inner_city_shp, berlin_shp, pr_stations, rScriptCommand);
                postprocessor.run();
            }

//            String runDirectory = "scenarios/output/runs-2023-05-26/extraPtPlan-false/drtStopBased-false/massConservation-true/";
//            //Cluster: scenarios/output/runs-2023-06-02/extraPtPlan-true/drtStopBased-true/massConservation-true/
//            String runId = "massConservation-1506vehicles-8seats";
//            //String runDirectory = "scenarios/output/runs-2023-06-13/baseCaseContinued-10pct/";
//            //String runId = "berlin-v5.5-10pct";
//            String population = runDirectory + runId + ".output_plans.xml.gz";
//
//
//            RunTripsPreparation postprocessor = new RunTripsPreparation(runDirectory, runId, population, inner_city_shp, berlin_shp, pr_stations, rScriptCommand);
//            postprocessor.run();
        } else {
            String runDirectory = args[0];
            String runId = args[1];
            String rScriptCommand = args[2];
            String population = runDirectory + runId + ".output_plans.xml.gz";
            String inner_city_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            String berlin_shp = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
            String pr_stations = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";

            RunTripsPreparation postprocessor = new RunTripsPreparation(runDirectory, runId, population, inner_city_shp, berlin_shp, pr_stations, rScriptCommand);
            postprocessor.run();
        }
    }

    public void run() {
        // Prepare selectedPlanScores.tsv
        // org.matsim.run.replaceCarByDRT.ScoresFromPlans2CSV.main(new String[]{runDirectory, population, inner_city_shp, berlin_shp, pr_stations});

        // Run all RScripts
        try {
            // Build the command for each R script
            String[] commands = {
//                    rScriptCommand, "src/main/R/policyTripsPreparation.R", runDirectory, runId,
                    rScriptCommand, "src/main/R/scoreComparison.R", runDirectory, runId,
                    rScriptCommand, "src/main/R/tripsComparison.R", runDirectory, runId,
                    rScriptCommand, "src/main/R/createOverviewTable.R", runDirectory, runId
            };



            // Execute each R script sequentially
            for (int i = 0; i < commands.length; i += 4){
                // Build the command for the current script
                String[] command = { commands[i], commands[i + 1], commands[i + 2], commands[i + 3] };

                // Create the process builder
                ProcessBuilder processBuilder = new ProcessBuilder(command);

                // Start the process
                Process process = processBuilder.start();

                // Get the output and error streams of the process
                InputStream inputStream = process.getInputStream();
                InputStream errorStream = process.getErrorStream();

                // Read the output stream
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

                // Read the error stream
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    System.err.println(errorLine);
                }

                // Wait for the process to complete
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    System.out.println("R script executed successfully.");
                } else {
                    System.out.println("R script execution failed. Exit code: " + exitCode);
                }

            }
        }  catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
