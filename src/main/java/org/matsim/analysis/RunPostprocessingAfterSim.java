package org.matsim.analysis;

import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.net.URL;

/**
 * This class can be used for postprocessing & analysing policy runs of this scenario. It combines all analyzing scripts & methods.
 *
 * Note: This class runs only for policyCases. The baseCase can be fully postprocessed except for the accident analysis.
 *       The airPollution analysis requires command line arguments & an encryptions key for the HBEFA tables.
 *       The RScripts scoreComparison & tripsComparison might not work on the HPC cluster. Two errors:
 *          - sf library not working (units package) on HPC cluster
 *          - matsim R library can´t be installed on HPC cluster
 */

public class RunPostprocessingAfterSim {
    private final String INPUT_RUNDIRECTORY;
    private final String INPUT_RUNID;
    private final String INPUT_BASE_RUNDIRECTORY;
    private final String INPUT_BASE_RUNID;
    private final String INPUT_RSCRIPTCOMMAND;
    private final String INPUT_INNERCITYSHP;
    private final String INPUT_BERLINSHP;
    private final String INPUT_PRSTATIONS;
    private final URL INPUT_PRSTATIONS_2;
    private final String INPUT_BOUNDARYSHP;
    private final String INPUT_ROADTYPESCARALLOWED;
    private final String INPUT_REPLACINGMODES;
    private final String INPUT_DRTSTOPS;

    public RunPostprocessingAfterSim(String runDirectory, String runId, String base_runDirectory, String base_runId, String rScriptCommand, String inner_city_shp, String berlin_shp, String pr_stations, String boundary_shp,
                                     String roadtypesCarAllowed, String replacingModes, String drtStops){
        this.INPUT_RUNDIRECTORY = runDirectory;
        this.INPUT_RUNID = runId;
        this.INPUT_BASE_RUNDIRECTORY = base_runDirectory;
        this.INPUT_BASE_RUNID = base_runId;
        this.INPUT_RSCRIPTCOMMAND = rScriptCommand;
        this.INPUT_INNERCITYSHP = inner_city_shp;
        this.INPUT_BERLINSHP = berlin_shp;
        this.INPUT_PRSTATIONS = pr_stations;
        this.INPUT_BOUNDARYSHP = boundary_shp;
        this.INPUT_PRSTATIONS_2 = IOUtils.resolveFileOrResource(pr_stations);
        this.INPUT_ROADTYPESCARALLOWED = roadtypesCarAllowed;
        this.INPUT_REPLACINGMODES = replacingModes;
        this.INPUT_DRTSTOPS = drtStops;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            String runDirectory = "scenarios/output/runs-2023-09-01/10pct/roadtypesAllowed-all/";
            String runId = "roadtypesAllowed-all";
            String base_runDirectory = "scenarios/output/baseCaseContinued-10pct/";
            String base_runId = "berlin-v5.5-10pct";
            String rScriptCommand = "C:/Program Files/R/R-4.2.2/bin/Rscript.exe";
            String inner_city_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            String berlin_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/berlin.shp";
            String pr_stations = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-07-27-pr-stations.tsv";
            String boundary_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-boundaries-500m.shp";
            String roadtypesCarAllowed = "motorwayAndPrimaryAndTrunk";
            String replacingModes = "pt,drt";
            String drtStops = "scenarios/berlin/replaceCarByDRT/noModeChoice/drtStops/drtStops-hundekopf-carBanArea-2023-07-27-prStations.xml";

            RunPostprocessingAfterSim postprocessorAfterSim = new RunPostprocessingAfterSim(runDirectory, runId, base_runDirectory, base_runId, rScriptCommand, inner_city_shp, berlin_shp, pr_stations, boundary_shp,
                    roadtypesCarAllowed, replacingModes, drtStops);
            postprocessorAfterSim.run();

        } else {
            String runDirectory = args[0];
            String runId = args[1];
            String base_runDirectory = args[2];
            String base_runId = args[3];
            String rScriptCommand = args[4];
            String inner_city_shp = args[5];
            String berlin_shp = args[6];
            String pr_stations = args[7];
            String boundary_shp = args[8];
            String roadtypesCarAllowed = args[9];
            String replacingModes = args[10];
            String drtStops = args[11];

            RunPostprocessingAfterSim postprocessorAfterSim = new RunPostprocessingAfterSim(runDirectory, runId, base_runDirectory, base_runId, rScriptCommand, inner_city_shp, berlin_shp, pr_stations, boundary_shp,
                    roadtypesCarAllowed, replacingModes, drtStops);
            postprocessorAfterSim.run();
        }
    }

    public void run() {
        String population = INPUT_RUNDIRECTORY + INPUT_RUNID + ".output_plans.xml.gz";
        String hbefaWarmFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/944637571c833ddcf1d0dfcccb59838509f397e6.enc";
        String hbefaColdFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/54adsdas478ss457erhzj5415476dsrtzu.enc";
        String runType = "policy"; // assuming we´re only using postprocessing for policy runs -> accidents for the baseCaseContinued needs to be run separately

        // Part 1: Postprocessing simulation, generating results for score/trips/prStations/trafficVolumes
        RunScorePreparation scorePreparation = new RunScorePreparation(INPUT_RUNDIRECTORY, population, INPUT_INNERCITYSHP, INPUT_BERLINSHP, INPUT_PRSTATIONS_2, INPUT_BOUNDARYSHP);
        scorePreparation.run();

        RunTripsPreparation tripsPreparation = new RunTripsPreparation(INPUT_RUNDIRECTORY, INPUT_RUNID, INPUT_INNERCITYSHP, INPUT_BERLINSHP, INPUT_PRSTATIONS, INPUT_RSCRIPTCOMMAND);
        tripsPreparation.run();

        RunPRActivityEventHandler prActivityPreparation = new RunPRActivityEventHandler(INPUT_RUNDIRECTORY, INPUT_RUNID, INPUT_PRSTATIONS_2);
        prActivityPreparation.run();

        RunTrafficVolumeEventHandler trafficVolumePreparation = new RunTrafficVolumeEventHandler(INPUT_RUNDIRECTORY, INPUT_RUNID, INPUT_INNERCITYSHP, INPUT_BERLINSHP, INPUT_BOUNDARYSHP);
        trafficVolumePreparation.run();

        RunRPostprocessing rPostprocessing = new RunRPostprocessing(INPUT_RUNDIRECTORY, INPUT_RUNID, INPUT_INNERCITYSHP, INPUT_BERLINSHP, INPUT_PRSTATIONS, INPUT_RSCRIPTCOMMAND, INPUT_BASE_RUNDIRECTORY, INPUT_BASE_RUNID);
        rPostprocessing.run();

        // Part 2: Analyzing & postprocessing externalities -> accidents/airPollution/noise
        BerlinRunOfflineNoiseAnalysis.main(new String[]{INPUT_RUNDIRECTORY, INPUT_RUNID, INPUT_RUNDIRECTORY + "analysis/noise/"});
        BerlinRunOnlineAccidents.main(new String[]{INPUT_RUNDIRECTORY, INPUT_RUNID, INPUT_RUNDIRECTORY + "analysis/accidents/", runType, INPUT_INNERCITYSHP, INPUT_ROADTYPESCARALLOWED, INPUT_PRSTATIONS, INPUT_REPLACINGMODES, INPUT_DRTSTOPS});

        // Note: AirPollutionAnalysis requires two things: command line arguments & Encryption key for HBEFA values
        try {
            BerlinOfflineAirPollutionAnalysisByEngineInformation.main(new String[]{INPUT_RUNDIRECTORY, INPUT_RUNID, hbefaWarmFile, hbefaColdFile, INPUT_RUNDIRECTORY + "analysis/airPollution/"});
        } catch (IOException e) {
            e.printStackTrace();
        }

        RunRPostprocessingEcon rPostprocessingEcon = new RunRPostprocessingEcon(INPUT_RUNDIRECTORY, INPUT_RUNID, INPUT_INNERCITYSHP, INPUT_BERLINSHP, INPUT_PRSTATIONS, INPUT_RSCRIPTCOMMAND, INPUT_BASE_RUNDIRECTORY, INPUT_BASE_RUNID);
        rPostprocessingEcon.run();

    }
}
