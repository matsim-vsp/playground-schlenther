package org.matsim.run.replaceCarByDRT;

import org.matsim.analysis.RunComparisonAnalysis;
import org.matsim.analysis.RunEconomicAnalysis;
import org.matsim.analysis.RunTripsPreparation;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

import java.io.IOException;


public class PostprocessingListener implements ShutdownListener {

    private Controler controler;

    public PostprocessingListener(Controler controler) {
        this.controler = controler;
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        Config config = controler.getConfig();

        String runDirectory = config.controler().getOutputDirectory();
        String runId = config.controler().getRunId();
        String rScriptCommand = "C:/Program Files/R/R-4.2.2/bin/Rscript.exe";
        String population = runDirectory + runId + ".output_plans.xml.gz";
        String inner_city_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
        String berlin_shp = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
        String pr_stations = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";
        String boundary_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-boundaries-500m.shp";

        RunScorePreparation scorePreparation = new RunScorePreparation(runDirectory, population, inner_city_shp, berlin_shp, pr_stations, boundary_shp);
        scorePreparation.run();

        RunTripsPreparation tripsPreparation = new RunTripsPreparation(runDirectory, runId, population, inner_city_shp, berlin_shp, pr_stations, rScriptCommand);
        tripsPreparation.run();

        RunPRActivityEventHandler prActivities = new RunPRActivityEventHandler(runDirectory, runId, pr_stations);
        prActivities.run();

        RunTrafficVolumeEventHandler trafficVolumes = new RunTrafficVolumeEventHandler(runDirectory, runId, inner_city_shp, berlin_shp);
        trafficVolumes.run();

        // Part 2: All the analyses -> Comparison, Economics, doesn´t work on cluster yet, so this can only be used locally!
        // if Economics is added, PW for HBEFA files & Command Line Arguments for AirPollutionAnalysis must be added
//        String runType = "policy";
//        String roadTypesCarAllowed = "motorwayAndPrimaryAndTrunk";
//        String prStationChoice = "closestToOutSideActivity";
//        String replacingModes = "pt,drt";
//        String enforceMassConservation = "true";
//        String extraPtPlan = "false";
//        String drtStopBased = "false";
//        String drtStops = "scenarios/berlin/replaceCarByDRT/noModeChoice/drtStops/drtStops-hundekopf-carBanArea-2023-03-29-prStations.xml";
//        String extraPRStationChoice = "closestToOutSideActivity";
//
//        String hbefaWarmFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/944637571c833ddcf1d0dfcccb59838509f397e6.enc";
//        String hbefaColdFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/54adsdas478ss457erhzj5415476dsrtzu.enc";
//
//        RunComparisonAnalysis comparisonAnalysis = new RunComparisonAnalysis(runDirectory, runId, population, inner_city_shp, berlin_shp, pr_stations, rScriptCommand);
//        comparisonAnalysis.run();
//
//        RunEconomicAnalysis economicAnalysis = new RunEconomicAnalysis(runDirectory, runId, runType, inner_city_shp, roadTypesCarAllowed, pr_stations, prStationChoice, replacingModes, enforceMassConservation, extraPtPlan, drtStopBased, drtStops, extraPRStationChoice, hbefaWarmFile, hbefaColdFile);
//        try {
//            economicAnalysis.run();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }
}
