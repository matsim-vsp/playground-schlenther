package org.matsim.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.noise.MergeNoiseCSVFile;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class RunOfflineNoiseAnalysisWithDrt {
    private static final Logger log = Logger.getLogger(RunOfflineNoiseAnalysis.class);
    private final String runDirectory;
    private final String runId;
    private final String analysisOutputDirectory;
    private final String tunnelLinkIdFile = null;
    private final String noiseBarriersFile = null;

    public RunOfflineNoiseAnalysisWithDrt(String runDirectory, String runId, String analysisOutputDirectory) {
        this.runDirectory = runDirectory;
        this.runId = runId;
        if (!analysisOutputDirectory.endsWith("/")) {
            analysisOutputDirectory = analysisOutputDirectory + "/";
        }

        this.analysisOutputDirectory = analysisOutputDirectory;
    }

    public static void main(String[] args) {
        String runDirectory = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/";
        String runId = "berlin-v5.4-1pct";
        RunOfflineNoiseAnalysis analysis = new RunOfflineNoiseAnalysis("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/", "berlin-v5.4-1pct", "./scenario/");
        analysis.run();
    }

    void run() {
        double receiverPointGap = 100.0D;
        double timeBinSize = 3600.0D;
        Config config = ConfigUtils.createConfig(new ConfigGroup[]{new NoiseConfigGroup()});
        config.global().setCoordinateSystem("EPSG:31468");
        config.network().setInputFile(this.runDirectory + this.runId + ".output_network.xml.gz");
        config.plans().setInputFile(this.runDirectory + this.runId + ".output_plans.xml.gz");
        config.controler().setOutputDirectory(this.runDirectory);
        config.controler().setRunId(this.runId);
        NoiseConfigGroup noiseParameters = (NoiseConfigGroup)config.getModules().get("noise");
        noiseParameters.setReceiverPointGap(receiverPointGap);
        double xMin = 4573258.0D;
        double yMin = 5801225.0D;
        double xMax = 4620323.0D;
        double yMax = 5839639.0D;
        noiseParameters.setReceiverPointsGridMinX(xMin);
        noiseParameters.setReceiverPointsGridMinY(yMin);
        noiseParameters.setReceiverPointsGridMaxX(xMax);
        noiseParameters.setReceiverPointsGridMaxY(yMax);
        String[] consideredActivitiesForDamages = new String[]{"home*", "work*", "leisure*", "shopping*", "other*"};
        noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivitiesForDamages);
        noiseParameters.setUseActualSpeedLevel(false);
        noiseParameters.setAllowForSpeedsOutsideTheValidRange(false);
        noiseParameters.setScaleFactor(100.0D);
        noiseParameters.setComputePopulationUnits(true);
        noiseParameters.setComputeNoiseDamages(true);
        noiseParameters.setInternalizeNoiseDamages(false);
        noiseParameters.setComputeCausingAgents(false);
        noiseParameters.setThrowNoiseEventsAffected(true);
        noiseParameters.setThrowNoiseEventsCaused(false);
        String[] hgvIdPrefixes = new String[]{"freight"};
        noiseParameters.setHgvIdPrefixesArray(hgvIdPrefixes);
        noiseParameters.setTunnelLinkIdFile(this.tunnelLinkIdFile);
        noiseParameters.setTimeBinSizeNoiseComputation(timeBinSize);
        noiseParameters.setConsiderNoiseBarriers(false);
        noiseParameters.setNoiseBarriersFilePath(this.noiseBarriersFile);
        noiseParameters.setNoiseBarriersSourceCRS("EPSG:31468");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, this.analysisOutputDirectory);
        noiseCalculation.run();
        String outputFilePath = this.analysisOutputDirectory + "noise-analysis/";
        ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "immissions/", outputFilePath + "receiverPoints/receiverPoints.csv", noiseParameters.getReceiverPointGap());
        process.run();
        String[] labels = new String[]{"damages_receiverPoint"};
        String[] workingDirectories = new String[]{outputFilePath + "/damages_receiverPoint/"};
        MergeNoiseCSVFile merger = new MergeNoiseCSVFile();
        merger.setReceiverPointsFile(outputFilePath + "receiverPoints/receiverPoints.csv");
        merger.setOutputDirectory(outputFilePath);
        merger.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
        merger.setWorkingDirectory(workingDirectories);
        merger.setLabel(labels);
        merger.run();
        log.info("Done.");
    }
}
