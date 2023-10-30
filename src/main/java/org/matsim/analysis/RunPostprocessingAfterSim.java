package org.matsim.analysis;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.replaceCarByDRT.RunPRActivityEventHandler;
import org.matsim.run.replaceCarByDRT.RunScorePreparation;
import org.matsim.run.replaceCarByDRT.RunTrafficVolumeEventHandler;

import java.net.URL;

public class RunPostprocessingAfterSim {
    private final String INPUT_RUNDIRECTORY;
    private final String INPUT_RUNID;
    private final String INPUT_RSCRIPTCOMMAND;
    private final String INPUT_INNERCITYSHP;
    private final String INPUT_BERLINSHP;
    private final String INPUT_PRSTATIONS;
    private final URL INPUT_PRSTATIONS_2;
    private final String INPUT_BOUNDARYSHP;



    public RunPostprocessingAfterSim(String runDirectory, String runId, String rScriptCommand, String inner_city_shp, String berlin_shp, String pr_stations, String boundary_shp){
        this.INPUT_RUNDIRECTORY = runDirectory;
        this.INPUT_RUNID = runId;
        this.INPUT_RSCRIPTCOMMAND = rScriptCommand;
        this.INPUT_INNERCITYSHP = inner_city_shp;
        this.INPUT_BERLINSHP = berlin_shp;
        this.INPUT_PRSTATIONS = pr_stations;
        this.INPUT_BOUNDARYSHP = boundary_shp;
        this.INPUT_PRSTATIONS_2 = IOUtils.resolveFileOrResource(pr_stations);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            String runDirectory = "scenarios/output/runs-2023-09-01/1pct/extraPrStationPlan-true/";
            String runId = "extraPrStationPlan-true";
            String rScriptCommand = "C:/Program Files/R/R-4.2.2/bin/Rscript.exe";
            String inner_city_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            String berlin_shp = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
            String pr_stations = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-08-12-pr-stations-postprocessing.tsv";
            String boundary_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-boundaries-500m.shp";

            RunPostprocessingAfterSim postprocessorAfterSim = new RunPostprocessingAfterSim(runDirectory, runId, rScriptCommand, inner_city_shp, berlin_shp, pr_stations, boundary_shp);
            postprocessorAfterSim.run();

        } else {
            String runDirectory = args[0];
            String runId = args[1];
            String rScriptCommand = args[2];
            String inner_city_shp = args[3];
            String berlin_shp = args[4];
            String pr_stations = args[5];
            String boundary_shp = args[6];

            RunPostprocessingAfterSim postprocessorAfterSim = new RunPostprocessingAfterSim(runDirectory, runId, rScriptCommand, inner_city_shp, berlin_shp, pr_stations, boundary_shp);
            postprocessorAfterSim.run();
        }
    }

    public void run() {
        String population = INPUT_RUNDIRECTORY + INPUT_RUNID + ".output_plans.xml.gz";

        RunScorePreparation scorePreparation = new RunScorePreparation(INPUT_RUNDIRECTORY, population, INPUT_INNERCITYSHP, INPUT_BERLINSHP, INPUT_PRSTATIONS_2, INPUT_BOUNDARYSHP);
        scorePreparation.run();

        RunTripsPreparation tripsPreparation = new RunTripsPreparation(INPUT_RUNDIRECTORY, INPUT_RUNID, population, INPUT_INNERCITYSHP, INPUT_BERLINSHP, INPUT_PRSTATIONS, INPUT_RSCRIPTCOMMAND);
        tripsPreparation.run();

        RunPRActivityEventHandler prActivities = new RunPRActivityEventHandler(INPUT_RUNDIRECTORY, INPUT_RUNID, INPUT_PRSTATIONS_2);
        prActivities.run();

        RunTrafficVolumeEventHandler trafficVolumes = new RunTrafficVolumeEventHandler(INPUT_RUNDIRECTORY, INPUT_RUNID, INPUT_INNERCITYSHP, INPUT_BERLINSHP, INPUT_BOUNDARYSHP);
        trafficVolumes.run();

    }
}
