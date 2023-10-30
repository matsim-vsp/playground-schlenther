package org.matsim.analysis;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RunRPostprocessing {

    private final String INPUT_RUNDIRECTORY;
    private final String INPUT_RUNID;
    private final String INPUT_INNERCITYSHP;
    private final String INPUT_BERLINSHP;
    private final String INPUT_PRSTATIONS;
    private final String INPUT_RSCRIPTCOMMAND;
    private final String INPUT_BASE_RUNDIRECTORY;
    private final String INPUT_BASE_RUNID;

    public RunRPostprocessing(String runDirectory, String runId, String inner_city_shp, String berlin_shp, String pr_stations, String rScriptCommand, String base_runDirectory, String base_runId) {
        this.INPUT_RUNDIRECTORY = runDirectory;
        this.INPUT_RUNID = runId;
        this.INPUT_INNERCITYSHP = inner_city_shp;
        this.INPUT_BERLINSHP = berlin_shp;
        this.INPUT_PRSTATIONS = pr_stations;
        this.INPUT_RSCRIPTCOMMAND = rScriptCommand;
        this.INPUT_BASE_RUNDIRECTORY = base_runDirectory;
        this.INPUT_BASE_RUNID = base_runId;
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        if ( args.length==0 ){
            String rScriptCommand = "C:/Program Files/R/R-4.2.2/bin/Rscript.exe";
            String inner_city_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            String berlin_shp = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
            String pr_stations = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";
            String base_runDirectory = "";
            String base_runId = "";

            List<String> sensitivityRuns = new ArrayList<String>();
            sensitivityRuns.add("extraPtPlan-true");


            for(String run : sensitivityRuns) {
                String runId = run;
                String runDirectory = "scenarios/output/runs-2023-09-01/1pct/" + run + "/";

                RunRPostprocessing postprocessor = new RunRPostprocessing(runDirectory, runId, inner_city_shp, berlin_shp, pr_stations, rScriptCommand, base_runDirectory, base_runId);
                postprocessor.run();
            }

        } else {
            String runDirectory = args[0];
            String runId = args[1];
            String rScriptCommand = args[2];
            String inner_city_shp = args[3];
            String berlin_shp = args[4];
            String pr_stations = args[5];
            String base_runDirectory = args[6];
            String base_runId = args[7];

            RunRPostprocessing postprocessor = new RunRPostprocessing(runDirectory, runId, inner_city_shp, berlin_shp, pr_stations, rScriptCommand, base_runDirectory, base_runId);
            postprocessor.run();
        }
    }

    public void run() {

        // Run all RScripts
        try {
            // Build the command for each R script
            String[] commands = {
                    INPUT_RSCRIPTCOMMAND, "src/main/R/scoreComparison.R", INPUT_RUNDIRECTORY, INPUT_RUNID, INPUT_BASE_RUNDIRECTORY, INPUT_BASE_RUNID, INPUT_INNERCITYSHP, INPUT_BERLINSHP,
                    INPUT_RSCRIPTCOMMAND, "src/main/R/tripsComparison.R", INPUT_RUNDIRECTORY, INPUT_RUNID, INPUT_BASE_RUNDIRECTORY, INPUT_BASE_RUNID, INPUT_INNERCITYSHP, INPUT_BERLINSHP,
                    INPUT_RSCRIPTCOMMAND, "src/main/R/createOverviewTable.R", INPUT_RUNDIRECTORY, INPUT_RUNID, INPUT_BASE_RUNDIRECTORY, INPUT_BASE_RUNID, INPUT_INNERCITYSHP, INPUT_BERLINSHP,
                    INPUT_RSCRIPTCOMMAND, "src/main/R/differences_DTV.R", INPUT_RUNDIRECTORY, INPUT_RUNID, INPUT_BASE_RUNDIRECTORY, INPUT_BASE_RUNID, INPUT_INNERCITYSHP, INPUT_BERLINSHP,
                    INPUT_RSCRIPTCOMMAND, "src/main/R/prStationPlots.R", INPUT_RUNDIRECTORY, INPUT_RUNID, INPUT_BASE_RUNDIRECTORY, INPUT_BASE_RUNID, INPUT_INNERCITYSHP, INPUT_BERLINSHP,
            };

            for (int i = 0; i < commands.length; i += 8) {

                String[] command = { commands[i], commands[i + 1], commands[i + 2], commands[i + 3], commands[i+4], commands[i+5], commands[i+6], commands[i+7] };

                // Create the process builder, start process, get output & error streams
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                Process process = processBuilder.start();
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
