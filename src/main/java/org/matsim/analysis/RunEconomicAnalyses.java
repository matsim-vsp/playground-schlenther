package org.matsim.analysis;

public class RunEconomicAnalyses {

    private static final String runDirectory = "scenarios/output/runs-2023-05-26/extraPtPlan-false/drtStopBased-false/massConservation-true/";
    private static final String runId = "massConservation-1506vehicles-8seats";
    private static final String hbefaWarmFile = "";
    private static final String hbefaColdFile = "";
    private static final String analysisOutputDirectory = "scenarios/output/runs-2023-05-26/extraPtPlan-false/drtStopBased-false/massConservation-true/analysis/noise/";


    public static void main(String[] args) {
        RunOfflineNoiseAnalysis.main(new String[]{runDirectory, runId, analysisOutputDirectory});
        RunOfflineAirPollutionAnalysis.main(new String[]{runDirectory, runId, hbefaWarmFile, hbefaColdFile, analysisOutputDirectory});
        // & Berlin Accidents. still missing
    }

}
