package org.matsim.analysis;

import org.apache.log4j.Logger;
import org.matsim.run.replaceCarByDRT.RunBerlinNoInnerCarTripsScenario;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RunPostprocessing {

    private final String runDirectory;
    private final String runId;
    private final String population;
    private final String inner_city_shp;
    private final String berlin_shp;
    private final String pr_stations;

    private static final Logger log = Logger.getLogger(RunPostprocessing.class);

    public RunPostprocessing(String runDirectory, String runId, String population, String inner_city_shp, String berlin_shp, String pr_stations) {
        this.runDirectory = runDirectory;
        this.runId = runId;
        this.population = population;
        this.inner_city_shp = inner_city_shp;
        this.berlin_shp = berlin_shp;
        this.pr_stations = pr_stations;
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        if ( args.length==0 ){
            String runDirectory = "scenarios/output/runs-2023-06-13/finalRun-10pct/massConservation-true/";
            //Cluster: scenarios/output/runs-2023-06-02/extraPtPlan-true/drtStopBased-true/massConservation-true/
            String runId = "final-10pct-7503vehicles-8seats";
            //String runDirectory = "scenarios/output/runs-2023-06-13/baseCaseContinued-10pct/";
            //String runId = "berlin-v5.5-10pct";

            //String runDirectory = "scenarios/output/sample/";
            //String runId = "sample-run";
            String population = runDirectory + runId + ".output_plans.xml.gz";
            String inner_city_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            String berlin_shp = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
            String pr_stations = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";

            RunPostprocessing postprocessor = new RunPostprocessing(runDirectory, runId, population, inner_city_shp, berlin_shp, pr_stations);
            postprocessor.run();
        } else {
            String runDirectory = args[0];
            String runId = args[1];
            String population = runDirectory + runId + ".output_plans.xml.gz";
            String inner_city_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            String berlin_shp = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
            String pr_stations = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";

            RunPostprocessing postprocessor = new RunPostprocessing(runDirectory, runId, population, inner_city_shp, berlin_shp, pr_stations);
            postprocessor.run();
        }
    }

    public void run() {

        // Prepare selectedPlanScores.tsv
        // org.matsim.run.replaceCarByDRT.ScoresFromPlans2CSV.main(new String[]{runDirectory, population, inner_city_shp, berlin_shp, pr_stations});

        // Run all RScripts
        try {
            // TODO: Add Traffic Volume Analysis once done - probably not really needed
            // TODO: Make R work on cluster

            // Build the command for each R script
            String[] commands = {
                    //"C:/Program Files/R/R-4.2.2/bin/Rscript.exe", "src/main/R/policyTripsPreparation.R", runDirectory,
                    //"C:/Program Files/R/R-4.2.2/bin/Rscript.exe", "src/main/R/scoreComparison_V2.R", runDirectory,
                    "C:/Program Files/R/R-4.2.2/bin/Rscript.exe", "src/main/R/tripsComparison_V2.R", runDirectory,
            };



            // Execute each R script sequentially
            for (int i = 0; i < commands.length; i += 3){
                // Build the command for the current script
                String[] command = { commands[i], commands[i + 1], commands[i + 2] };

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
