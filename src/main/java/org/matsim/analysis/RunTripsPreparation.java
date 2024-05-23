package org.matsim.analysis;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RunTripsPreparation {

    private final String INPUT_RUNDIRECTORY;
    private final String INPUT_RUNID;
    private final String INPUT_INNERCITYSHP;
    private final String INPUT_BERLINSHP;
    private final String INPUT_PRSTATIONS;
    private final String INPUT_RSCRIPTCOMMAND;

    public RunTripsPreparation(String runDirectory, String runId, String inner_city_shp, String berlin_shp, String pr_stations, String rScriptCommand) {
        this.INPUT_RUNDIRECTORY = runDirectory;
        this.INPUT_RUNID = runId;
        this.INPUT_INNERCITYSHP = inner_city_shp;
        this.INPUT_BERLINSHP = berlin_shp;
        this.INPUT_PRSTATIONS = pr_stations;
        this.INPUT_RSCRIPTCOMMAND = rScriptCommand;
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        if ( args.length==0 ){
            String rScriptCommand = "C:/Program Files/R/R-4.3.1/bin/Rscript.exe";
            String inner_city_shp = "scenarios/berlin-v6.1/shp/hundekopf-carBanArea-25832.shp";
            String berlin_shp = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.1/input/shp/Berlin_25832.shp";
            String pr_stations = "scenarios/berlin-v6.1/berlin-v6.1-pr-stations-ring.tsv";

            String runDirectory = "//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/schlenther/berlin/2024-berlin-autofrei/output-1pct/drtHndKpf1.5kV-prRing-ptBike/";
            String runId = "berlin-v6.1-drt";

//            List<String> sensitivityRuns = new ArrayList<String>();
//            sensitivityRuns.add("extraPtPlan-true");


//            for(String run : sensitivityRuns) {
//                String runId = run;
//                String runDirectory = "scenarios/output/runs-2023-09-01/1pct/" + run + "/";

                RunTripsPreparation postprocessor = new RunTripsPreparation(runDirectory, runId, inner_city_shp, berlin_shp, pr_stations, rScriptCommand);
                postprocessor.run();
//            }

        } else {
            String runDirectory = args[0];
            String runId = args[1];
            String rScriptCommand = args[2];
            String inner_city_shp = args[3];
            String berlin_shp = args[4];
            String pr_stations = args[5];

            RunTripsPreparation postprocessor = new RunTripsPreparation(runDirectory, runId, inner_city_shp, berlin_shp, pr_stations, rScriptCommand);
            postprocessor.run();
        }
    }

    public void run() {

        // Run all RScripts
        try {
            // Build the command for each R script
            String[] command = {
                    INPUT_RSCRIPTCOMMAND, "src/main/R/policyTripsPreparation.R", INPUT_RUNDIRECTORY, INPUT_PRSTATIONS
            };

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

        }  catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
