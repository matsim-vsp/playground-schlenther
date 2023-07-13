package org.matsim.analysis;

import java.io.File;
import java.io.IOException;

public class RunEconomicAnalysis {

    private final String runDirectory;
    private final String runId;
    private final String runType;
    private final String inner_city_shp;
    private final String roadTypesCarAllowed;
    private final String pr_stations;
    private final String prStationChoice;
    private final String replacingModes;
    private final String enforceMassConservation;
    private final String extraPtPlan;
    private final String drtStopBased;
    private final String drtStops;
    private final String hbefaWarmFile;
    private final String hbefaColdFile;
    private final String extraPRStationChoice;

    public RunEconomicAnalysis(String runDirectory, String runId, String runType, String inner_city_shp, String roadTypesCarAllowed, String pr_stations, String prStationChoice, String replacingModes, String enforceMassConservation, String extraPtPlan, String drtStopBased, String drtStops, String hbefaWarmFile, String hbefaColdFile, String extraPRStationChoice){
        this.runDirectory = runDirectory;
        this.runId = runId;
        this.runType = runType;
        this.inner_city_shp = inner_city_shp;
        this.roadTypesCarAllowed = roadTypesCarAllowed;
        this.pr_stations = pr_stations;
        this.prStationChoice = prStationChoice;
        this.replacingModes = replacingModes;
        this.enforceMassConservation = enforceMassConservation;
        this.extraPtPlan = extraPtPlan;
        this.drtStopBased = drtStopBased;
        this.drtStops = drtStops;
        this.hbefaWarmFile = hbefaWarmFile;
        this.hbefaColdFile = hbefaColdFile;
        this.extraPRStationChoice = extraPRStationChoice;
    }

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            String runDirectory = "scenarios/output/sample/";
            String runId = "sample-run";
            String runType = "policy";

            String inner_city_shp = "scenarios/berlin/replaceCarByDRT/noModeChoice/shp/hundekopf-carBanArea.shp";
            String roadTypesCarAllowed = "motorwayAndPrimaryAndTrunk";
            String pr_stations = "scenarios/berlin/replaceCarByDRT/noModeChoice/prStations/2023-03-29-pr-stations.tsv";
            String prStationChoice = "closestToOutSideActivity";
            String replacingModes = "pt,drt";
            String enforceMassConservation = "true";
            String extraPtPlan = "false";
            String drtStopBased = "false";
            String drtStops = "scenarios/berlin/replaceCarByDRT/noModeChoice/drtStops/drtStops-hundekopf-carBanArea-2023-03-29-prStations.xml";
            String extraPRStationChoice = "closestToOutSideActivity";

            String hbefaWarmFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/944637571c833ddcf1d0dfcccb59838509f397e6.enc";
            String hbefaColdFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/54adsdas478ss457erhzj5415476dsrtzu.enc";

            RunEconomicAnalysis economicAnalysis = new RunEconomicAnalysis(runDirectory, runId, runType, inner_city_shp, roadTypesCarAllowed, pr_stations, prStationChoice, replacingModes, enforceMassConservation, extraPtPlan, drtStopBased, drtStops, hbefaWarmFile, hbefaColdFile, extraPRStationChoice);
            economicAnalysis.run();

        } else {
            String runDirectory = args[0];
            String runId = args[1];
            String runType = args[2];

            String inner_city_shp = args[3];
            String roadTypesCarAllowed = args[4];
            String pr_stations = args[5];
            String prStationChoice = args[6];
            String replacingModes = args[7];
            String enforceMassConservation = args[8];
            String extraPtPlan = args[9];
            String drtStopBased = args[10];
            String drtStops = args[11];
            String extraPRStationChoice = args[12];

            String hbefaWarmFile = args[13];
            String hbefaColdFile = args[14];

            RunEconomicAnalysis economicAnalysis = new RunEconomicAnalysis(runDirectory, runId, runType, inner_city_shp, roadTypesCarAllowed, pr_stations, prStationChoice, replacingModes, enforceMassConservation, extraPtPlan, drtStopBased, drtStops, hbefaWarmFile, hbefaColdFile, extraPRStationChoice);
            economicAnalysis.run();
        }

          }

    public void run() throws IOException {
        String noiseOutputDirectory = runDirectory + "analysis/noise/";
        String airPollutionOutputDirectory = runDirectory + "analysis/airPollution/";
        String accidentsOutputDirectory = runDirectory + "analysis/accidents/";

        File airPollutionDirectory = new File(airPollutionOutputDirectory);
        if(!airPollutionDirectory.exists()) {
            airPollutionDirectory.mkdirs();
        }
        BerlinRunOfflineNoiseAnalysis.main(new String[]{runDirectory, runId, noiseOutputDirectory});
        BerlinOfflineAirPollutionAnalysisByEngineInformation.main(new String[]{runDirectory, runId, hbefaWarmFile, hbefaColdFile, airPollutionOutputDirectory});
        BerlinRunOnlineAccidents.main(new String[]{runDirectory, runId, accidentsOutputDirectory, runType, inner_city_shp, roadTypesCarAllowed, pr_stations, prStationChoice, replacingModes, enforceMassConservation, extraPtPlan, drtStopBased, drtStops});
    }
}
