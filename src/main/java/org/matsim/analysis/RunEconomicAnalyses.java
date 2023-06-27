package org.matsim.analysis;

import org.matsim.contrib.emissions.EmissionUtils;

import java.io.File;
import java.io.IOException;

public class RunEconomicAnalyses {

    public static void main(String[] args) throws IOException {

        String runDirectory;
        String runId;
        String runType;
        String shapeFile;
        String roadTypesCarAllowed;
        String stationsFile;
        String prStationChoice;
        String replacingModes;
        String enforceMassConservation;
        String extraPtPlan;
        String drtStopBased;

        if (args.length == 0) {
            runDirectory = "scenarios/output/sample/";
            runId = "sample-run";
            runType = "policy";

            shapeFile = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            roadTypesCarAllowed = "motorwayAndPrimaryAndTrunk";
            stationsFile = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";
            prStationChoice = "closestToOutSideActivity";
            replacingModes = "pt,drt";
            enforceMassConservation = "true";
            extraPtPlan = "false";
            drtStopBased = "false";

        } else {
            runDirectory = args[0];
            runId = args[1];
            runType = args[2];

            shapeFile = args[3];
            roadTypesCarAllowed = args[4];
            stationsFile = args[5];
            prStationChoice = args[6];
            replacingModes = args[7];
            enforceMassConservation = args[8];
            extraPtPlan = args[9];
            drtStopBased = args[10];
        }

        String noiseOutputDirectory = runDirectory + "analysis/noise/";
        String airPollutionOutputDirectory = runDirectory + "analysis/airPollution/";
        String accidentsOutputDirectory = runDirectory + "analysis/accidents/";
        String hbefaWarmFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/944637571c833ddcf1d0dfcccb59838509f397e6.enc";
        String hbefaColdFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/54adsdas478ss457erhzj5415476dsrtzu.enc";

        File airPollutionDirectory = new File(airPollutionOutputDirectory);
        if(!airPollutionDirectory.exists()) {
            airPollutionDirectory.mkdirs();
        }

        RunOfflineNoiseAnalysisWithDrt.main(new String[]{runDirectory, runId, noiseOutputDirectory});
        RunOfflineAirPollutionAnalysisWithDrt.main(new String[]{runDirectory, runId, hbefaWarmFile, hbefaColdFile, airPollutionOutputDirectory});
        RunAccidentsWithDrt.main(new String[]{runDirectory, runId, accidentsOutputDirectory, runType, shapeFile, roadTypesCarAllowed, stationsFile, prStationChoice, replacingModes, enforceMassConservation, extraPtPlan, drtStopBased}); // Guice Injection Error
    }

}
