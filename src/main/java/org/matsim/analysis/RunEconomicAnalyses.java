package org.matsim.analysis;

import java.io.IOException;

public class RunEconomicAnalyses {

    private static String runDirectory;
    private static String runId;
    private static String hbefaWarmFile;
    private static String hbefaColdFile;
    private static String noiseOutputDirectory;
    private static String airPollutionOutputDirectory;
    private static String accidentsOutputDirectory;
    private static String runType;

    private static String shapeFile;
    private static String roadTypesCarAllowed;
    private static String stationsFile;
    private static String prStationChoice;
    private static String replacingModes;
    private static String enforceMassConservation;
    private static String extraPtPlan;
    private static String drtStopBased;


    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            runDirectory = "scenarios/output/runs-2023-05-26/extraPtPlan-false/drtStopBased-false/massConservation-true/";
            runId = "massConservation-1506vehicles-8seats";
            runType = "policy";

            shapeFile = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            roadTypesCarAllowed = "motorwayAndPrimaryAndTrunk";
            stationsFile = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";
            prStationChoice = "closestToOutSideActivity";
            replacingModes = "pt,drt";
            enforceMassConservation = "false";
            extraPtPlan = "true";
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

        noiseOutputDirectory = runDirectory + "analysis/noise/";
        airPollutionOutputDirectory = runDirectory + "analysis/airPollution/";
        accidentsOutputDirectory = runDirectory + "analysis/accidents/";
        hbefaWarmFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/f5b276f41a0531ed740a81f4615ec00f4ff7a28d.enc";
        hbefaColdFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/b63f949211b7c93776cdce8a7600eff4e36460c8.enc";

        RunOfflineNoiseAnalysisWithDrt.main(new String[]{runDirectory, runId, noiseOutputDirectory});
        // RunOfflineAirPollutionAnalysisWithDrt.main(new String[]{runDirectory, runId, hbefaWarmFile, hbefaColdFile, airPollutionOutputDirectory});
        RunAccidentsWithDrt.main(new String[]{runDirectory, runId, accidentsOutputDirectory, runType, shapeFile, roadTypesCarAllowed, stationsFile, prStationChoice, replacingModes, enforceMassConservation, extraPtPlan, drtStopBased});
    }

}
